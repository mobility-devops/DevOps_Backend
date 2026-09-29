package kim.autoever.taxi.vehicle.controller;

import kim.autoever.taxi.common.exception.GlobalExceptionHandler;
import kim.autoever.taxi.common.exception.RequestIdFilter;
import kim.autoever.taxi.user.auth.CurrentUserArgumentResolver;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
import kim.autoever.taxi.vehicle.dto.VehicleResponse;
import kim.autoever.taxi.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VehicleController(vehicleService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    private void loginAsDriver() {
        User user = User.create("홍길동", "01012345678", UserRole.DRIVER);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    private VehicleResponse response() {
        Instant now = Instant.parse("2026-09-30T00:00:00Z");
        return new VehicleResponse(5L, 10L, "12가3456", "쏘나타", now, now);
    }

    @Test
    void 차량을_등록하면_201과_Location을_반환한다() throws Exception {
        loginAsDriver();
        when(vehicleService.create(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\":\"12가3456\",\"model\":\"쏘나타\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/drivers/me/vehicle"))
                .andExpect(jsonPath("$.plateNumber").value("12가3456"));
    }

    @Test
    void 잘못된_입력이면_400과_필드_오류를_반환한다() throws Exception {
        loginAsDriver();

        mockMvc.perform(post("/api/v1/drivers/me/vehicle")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\":\"\",\"model\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void 차량을_조회한다() throws Exception {
        loginAsDriver();
        when(vehicleService.get(any())).thenReturn(response());

        mockMvc.perform(get("/api/v1/drivers/me/vehicle").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("쏘나타"));
    }

    @Test
    void 차량을_수정한다() throws Exception {
        loginAsDriver();
        when(vehicleService.update(any(), any())).thenReturn(response());

        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\":\"12가3456\",\"model\":\"쏘나타\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 차량을_삭제하면_204를_반환한다() throws Exception {
        loginAsDriver();

        mockMvc.perform(delete("/api/v1/drivers/me/vehicle").header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }
}