package kim.autoever.taxi.driver.controller;

import kim.autoever.taxi.common.exception.GlobalExceptionHandler;
import kim.autoever.taxi.common.exception.RequestIdFilter;
import kim.autoever.taxi.driver.domain.DriverAvailability;
import kim.autoever.taxi.driver.dto.DriverResponse;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.user.auth.CurrentUserArgumentResolver;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverService driverService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DriverController(driverService))
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

    private DriverResponse response(DriverAvailability availability) {
        Instant now = Instant.parse("2026-09-30T00:00:00Z");
        return new DriverResponse(10L, 1L, availability, now, now);
    }

    @Test
    void 내_기사_정보를_조회한다() throws Exception {
        loginAsDriver();
        when(driverService.getMe(any())).thenReturn(response(DriverAvailability.OFFLINE));

        mockMvc.perform(get("/api/v1/drivers/me").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("OFFLINE"));
    }

    @Test
    void 기사_상태를_변경한다() throws Exception {
        loginAsDriver();
        when(driverService.changeAvailability(any(), any())).thenReturn(response(DriverAvailability.ONLINE));

        mockMvc.perform(put("/api/v1/drivers/me/availability")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"ONLINE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("ONLINE"));
    }

    @Test
    void 정의되지_않은_상태면_400을_반환한다() throws Exception {
        loginAsDriver();

        mockMvc.perform(put("/api/v1/drivers/me/availability")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"BUSY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 헤더가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/me"))
                .andExpect(status().isUnauthorized());
    }
}