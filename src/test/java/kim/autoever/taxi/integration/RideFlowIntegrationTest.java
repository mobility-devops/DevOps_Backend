package kim.autoever.taxi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RideFlowIntegrationTest extends IntegrationTestSupport {

    private static String accept(long rideId) {
        return "/api/v1/rides/" + rideId + "/accept";
    }

    private static String arrive(long rideId) {
        return "/api/v1/rides/" + rideId + "/arrive";
    }

    private static String start(long rideId) {
        return "/api/v1/rides/" + rideId + "/start";
    }

    private static String complete(long rideId) {
        return "/api/v1/rides/" + rideId + "/complete";
    }

    private static String cancel(long rideId) {
        return "/api/v1/rides/" + rideId + "/cancel";
    }

    private static String ride(long rideId) {
        return "/api/v1/rides/" + rideId;
    }

    @Test
    void 호출부터_완료까지_전체_흐름을_처리한다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");

        long rideId = createRide(passenger);
        assertThat(rideStatus(rideId)).isEqualTo("SEARCHING");
        assertThat(count("active_passenger_rides")).isEqualTo(1);

        getAs(passenger, "/api/v1/rides/current")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ride.id").value(rideId))
                .andExpect(jsonPath("$.ride.status").value("SEARCHING"));
        getAs(driver, "/api/v1/rides?status=SEARCHING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(rideId));

        postAs(driver, accept(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driverId").isNumber());
        assertThat(count("active_assignments")).isEqualTo(1);
        getAs(driver, ride(rideId)).andExpect(status().isOk());
        getAs(passenger, ride(rideId)).andExpect(status().isOk());

        postAs(driver, arrive(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"))
                .andExpect(jsonPath("$.version").isNumber());
        postAs(driver, start(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        postAs(driver, complete(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(rideStatus(rideId)).isEqualTo("COMPLETED");
        assertThat(count("active_passenger_rides")).isZero();
        assertThat(count("active_assignments")).isZero();
        getAs(passenger, "/api/v1/rides/current")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ride").isEmpty());

        // 완료 후 승객은 다시 호출하고, 기사는 다른 호출을 수락할 수 있다.
        long nextRide = createRide(passenger);
        postAs(driver, accept(nextRide))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void 대기_중인_호출을_취소하면_승객이_다시_호출할_수_있다() throws Exception {
        long passenger = passenger("01011112222");
        long rideId = createRide(passenger);

        postAs(passenger, cancel(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(rideStatus(rideId)).isEqualTo("CANCELLED");
        assertThat(count("active_passenger_rides")).isZero();
        createRide(passenger);
    }

    @Test
    void 배정된_호출을_취소하면_기사가_다른_호출을_수락할_수_있다() throws Exception {
        long passenger = passenger("01011112222");
        long otherPassenger = passenger("01055556666");
        long driver = onlineDriver("01033334444");
        long rideId = createRide(passenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());
        postAs(driver, arrive(rideId)).andExpect(status().isOk());

        postAs(passenger, cancel(rideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(count("active_assignments")).isZero();
        assertThat(count("active_passenger_rides")).isZero();
        long otherRide = createRide(otherPassenger);
        postAs(driver, accept(otherRide))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        // 취소된 호출은 더 이상 수락·진행할 수 없다.
        postAs(driver, arrive(rideId)).andExpect(status().isConflict());
    }

    @Test
    void 운행_중이거나_끝난_호출은_취소할_수_없다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");
        long rideId = createRide(passenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());
        postAs(driver, arrive(rideId)).andExpect(status().isOk());
        postAs(driver, start(rideId)).andExpect(status().isOk());

        postAs(passenger, cancel(rideId)).andExpect(status().isConflict());
        assertThat(rideStatus(rideId)).isEqualTo("IN_PROGRESS");
        assertThat(count("active_assignments")).isEqualTo(1);

        postAs(driver, complete(rideId)).andExpect(status().isOk());
        postAs(passenger, cancel(rideId)).andExpect(status().isConflict());
        assertThat(rideStatus(rideId)).isEqualTo("COMPLETED");
    }

    @Test
    void 진행_중인_호출이_있으면_다시_호출할_수_없다() throws Exception {
        long passenger = passenger("01011112222");
        createRide(passenger);

        createRideRequest(passenger)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        assertThat(count("rides")).isEqualTo(1);
    }

    @Test
    void 이미_수락된_호출은_다른_기사가_수락할_수_없다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");
        long otherDriver = onlineDriver("01077778888");
        long rideId = createRide(passenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());

        postAs(otherDriver, accept(rideId)).andExpect(status().isConflict());
        assertThat(count("active_assignments")).isEqualTo(1);
    }

    @Test
    void OFFLINE_기사는_수락할_수_없다() throws Exception {
        long passenger = passenger("01011112222");
        long offlineDriver = driver("01033334444");
        long rideId = createRide(passenger);

        postAs(offlineDriver, accept(rideId)).andExpect(status().isConflict());
        assertThat(rideStatus(rideId)).isEqualTo("SEARCHING");
    }

    @Test
    void 운행_중인_기사는_다른_호출을_수락할_수_없다() throws Exception {
        long passenger = passenger("01011112222");
        long otherPassenger = passenger("01055556666");
        long driver = onlineDriver("01033334444");
        long rideId = createRide(passenger);
        long otherRide = createRide(otherPassenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());

        postAs(driver, accept(otherRide)).andExpect(status().isConflict());
        assertThat(rideStatus(otherRide)).isEqualTo("SEARCHING");
    }

    @Test
    void 운행_중인_기사는_OFFLINE으로_변경할_수_없고_완료_후에는_가능하다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");
        long rideId = createRide(passenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());

        changeAvailability(driver, "OFFLINE").andExpect(status().isConflict());

        postAs(driver, arrive(rideId)).andExpect(status().isOk());
        postAs(driver, start(rideId)).andExpect(status().isOk());
        postAs(driver, complete(rideId)).andExpect(status().isOk());
        changeAvailability(driver, "OFFLINE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("OFFLINE"));
    }

    @Test
    void 순서를_건너뛰거나_되돌리는_상태_변경은_409를_반환한다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");
        long rideId = createRide(passenger);
        postAs(driver, accept(rideId)).andExpect(status().isOk());

        postAs(driver, start(rideId)).andExpect(status().isConflict());
        postAs(driver, complete(rideId)).andExpect(status().isConflict());
        postAs(driver, arrive(rideId)).andExpect(status().isOk());
        postAs(driver, arrive(rideId)).andExpect(status().isConflict());
        assertThat(rideStatus(rideId)).isEqualTo("ARRIVED");
    }

    @Test
    void 권한이_없는_요청은_403을_반환한다() throws Exception {
        long passenger = passenger("01011112222");
        long otherPassenger = passenger("01055556666");
        long driver = onlineDriver("01033334444");
        long otherDriver = onlineDriver("01077778888");
        long rideId = createRide(passenger);

        // 승객은 수락·목록 조회 불가, 기사는 호출 생성·취소 불가
        postAs(passenger, accept(rideId)).andExpect(status().isForbidden());
        getAs(passenger, "/api/v1/rides?status=SEARCHING").andExpect(status().isForbidden());
        createRideRequest(driver).andExpect(status().isForbidden());
        postAs(driver, cancel(rideId)).andExpect(status().isForbidden());

        postAs(driver, accept(rideId)).andExpect(status().isOk());
        // 다른 승객·배정되지 않은 기사는 조회·변경·취소 불가
        getAs(otherPassenger, ride(rideId)).andExpect(status().isForbidden());
        getAs(otherDriver, ride(rideId)).andExpect(status().isForbidden());
        postAs(otherDriver, arrive(rideId)).andExpect(status().isForbidden());
        postAs(otherPassenger, cancel(rideId)).andExpect(status().isForbidden());
        assertThat(rideStatus(rideId)).isEqualTo("ASSIGNED");
    }

    @Test
    void 없는_호출_또는_사용자는_404를_반환한다() throws Exception {
        long passenger = passenger("01011112222");
        long driver = onlineDriver("01033334444");

        getAs(passenger, ride(999999)).andExpect(status().isNotFound());
        postAs(driver, accept(999999)).andExpect(status().isNotFound());
        postAs(driver, arrive(999999)).andExpect(status().isNotFound());
        postAs(passenger, cancel(999999)).andExpect(status().isNotFound());
        getAs(999999, "/api/v1/rides/current").andExpect(status().isNotFound());
    }

    @Test
    void X_User_Id_헤더가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rides/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RIDE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_좌표는_400을_반환한다() throws Exception {
        long passenger = passenger("01011112222");

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", passenger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickup":{"latitude":91,"longitude":126.9780},
                                 "destination":{"latitude":37.4979,"longitude":127.0276}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", passenger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickup":{"latitude":37.5665,"longitude":126.9780},
                                 "destination":{"latitude":37.4979,"longitude":-181}}"""))
                .andExpect(status().isBadRequest());
        assertThat(count("rides")).isZero();
    }
}
