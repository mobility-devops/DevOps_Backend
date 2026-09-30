package kim.autoever.taxi.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 요청에서 중복 수락·중복 호출이 막히는지 실제 MySQL 로 검증한다.
 * 실패한 요청은 500 이 아니라 409 여야 한다.
 */
class RideConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final int ROUNDS = 10;
    private static final int PARALLELISM = 5;

    /** 모든 작업을 동시에 시작시키고 HTTP 상태 코드 목록을 반환한다. */
    private List<Integer> runConcurrently(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 여러_기사가_동시에_같은_호출을_수락하면_한_명만_성공한다() throws Exception {
        long passenger = passenger("01011112222");
        List<Long> drivers = new ArrayList<>();
        for (int i = 0; i < PARALLELISM; i++) {
            drivers.add(onlineDriver("0103333000" + i));
        }

        for (int round = 0; round < ROUNDS; round++) {
            long rideId = createRide(passenger);

            List<Callable<Integer>> tasks = new ArrayList<>();
            for (long driver : drivers) {
                tasks.add(() -> postAs(driver, "/api/v1/rides/" + rideId + "/accept")
                        .andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = runConcurrently(tasks);

            assertThat(statuses).as("round %d", round)
                    .filteredOn(s -> s == 200).hasSize(1);
            assertThat(statuses).as("round %d 실패 응답은 모두 409", round)
                    .filteredOn(s -> s != 200).allMatch(s -> s == 409);
            assertThat(rideStatus(rideId)).isEqualTo("ASSIGNED");
            assertThat(count("active_assignments")).isEqualTo(1);

            // 다음 라운드를 위해 운행을 끝낸다.
            Long assigned = jdbcTemplate.queryForObject(
                    "SELECT u.id FROM rides r JOIN drivers d ON d.id = r.driver_id JOIN users u ON u.id = d.user_id WHERE r.id = ?",
                    Long.class, rideId);
            postAs(assigned, "/api/v1/rides/" + rideId + "/arrive").andReturn();
            postAs(assigned, "/api/v1/rides/" + rideId + "/start").andReturn();
            postAs(assigned, "/api/v1/rides/" + rideId + "/complete").andReturn();
            assertThat(count("active_assignments")).isZero();
        }
    }

    @Test
    void 한_기사가_동시에_여러_호출을_수락하면_한_건만_성공한다() throws Exception {
        long driver = onlineDriver("01033334444");
        List<Long> rides = new ArrayList<>();
        for (int i = 0; i < PARALLELISM; i++) {
            rides.add(createRide(passenger("0101111000" + i)));
        }

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (long rideId : rides) {
            tasks.add(() -> postAs(driver, "/api/v1/rides/" + rideId + "/accept")
                    .andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s != 200).allMatch(s -> s == 409);
        assertThat(count("active_assignments")).isEqualTo(1);
    }

    @Test
    void 한_승객이_동시에_여러_번_호출하면_한_건만_생성된다() throws Exception {
        long passenger = passenger("01011112222");

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < PARALLELISM; i++) {
            tasks.add(() -> createRideRequest(passenger).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s != 201).allMatch(s -> s == 409);
        assertThat(count("rides")).isEqualTo(1);
        assertThat(count("active_passenger_rides")).isEqualTo(1);
    }

    @Test
    void 취소와_수락이_동시에_와도_일관된_상태를_유지한다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");

        for (int round = 0; round < ROUNDS; round++) {
            long rideId = createRide(passenger);

            List<Callable<Integer>> tasks = List.of(
                    () -> postAs(driver, "/api/v1/rides/" + rideId + "/accept").andReturn().getResponse().getStatus(),
                    () -> postAs(passenger, "/api/v1/rides/" + rideId + "/cancel").andReturn().getResponse().getStatus());
            List<Integer> statuses = runConcurrently(tasks);

            assertThat(statuses).as("round %d", round).doesNotContain(500);
            String status = rideStatus(rideId);
            assertThat(status).isIn("ASSIGNED", "CANCELLED");
            if ("CANCELLED".equals(status)) {
                assertThat(count("active_assignments")).isZero();
                assertThat(count("active_passenger_rides")).isZero();
            } else {
                // 수락이 먼저 반영된 뒤 취소가 이어진 경우가 아니라면 배정 정보가 남아 있어야 한다.
                assertThat(count("active_assignments")).isEqualTo(1);
                postAs(passenger, "/api/v1/rides/" + rideId + "/cancel").andReturn();
            }
            assertThat(count("active_passenger_rides")).isZero();
            assertThat(count("active_assignments")).isZero();
        }
    }
}
