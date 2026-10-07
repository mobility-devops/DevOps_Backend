package kim.autoever.taxi.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import kim.autoever.taxi.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Testcontainers 로 띄운 MySQL 8.4 를 사용하는 통합 테스트 공통 설정.
 * 매 테스트 전에 모든 테이블을 비운다. Flyway 마이그레이션은 컨텍스트 시작 시 적용된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String RIDE_BODY = """
            {"pickup":{"latitude":37.5665,"longitude":126.9780,"address":"서울시청"},
             "destination":{"latitude":37.4979,"longitude":127.0276,"address":"강남역"}}""";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // FK 순서: 자식 테이블부터 삭제
        for (String table : new String[]{
                "active_assignments", "active_passenger_rides", "rides", "vehicles", "drivers", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    protected long signUp(String name, String phone, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result, "$.id");
    }

    protected long passenger(String phone) throws Exception {
        return signUp("승객" + phone, phone, "PASSENGER");
    }

    protected long driver(String phone) throws Exception {
        return signUp("기사" + phone, phone, "DRIVER");
    }

    protected long onlineDriver(String phone) throws Exception {
        long userId = driver(phone);
        changeAvailability(userId, "ONLINE").andExpect(status().isOk());
        return userId;
    }

    protected ResultActions changeAvailability(long userId, String availability) throws Exception {
        return mockMvc.perform(put("/api/v1/drivers/me/availability")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availability\":\"" + availability + "\"}"));
    }

    protected long createRide(long passengerUserId) throws Exception {
        MvcResult result = createRideRequest(passengerUserId)
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result, "$.id");
    }

    protected ResultActions createRideRequest(long passengerUserId) throws Exception {
        return mockMvc.perform(post("/api/v1/rides")
                .header("X-User-Id", passengerUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(RIDE_BODY));
    }

    protected ResultActions postAs(long userId, String url) throws Exception {
        return mockMvc.perform(post(url).header("X-User-Id", userId));
    }

    protected ResultActions getAs(long userId, String url) throws Exception {
        return mockMvc.perform(get(url).header("X-User-Id", userId));
    }

    protected long idOf(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return value.longValue();
    }

    protected int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    protected String rideStatus(long rideId) {
        return jdbcTemplate.queryForObject("SELECT status FROM rides WHERE id = ?", String.class, rideId);
    }
}
