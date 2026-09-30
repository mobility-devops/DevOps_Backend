package kim.autoever.taxi.ride.controller;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.GlobalExceptionHandler;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.common.exception.RequestIdFilter;
import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.LocationResponse;
import kim.autoever.taxi.ride.dto.RideResponse;
import kim.autoever.taxi.ride.dto.RideStatusResponse;
import kim.autoever.taxi.ride.service.RideService;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    private static final String VALID_BODY = """
            {"pickup":{"latitude":37.5665,"longitude":126.9780,"address":"서울시청"},
             "destination":{"latitude":37.4979,"longitude":127.0276}}""";

    @Mock
    private RideService rideService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RideController(rideService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    private void login(long id, UserRole role) {
        User user = User.create("사용자", "0101234" + id, role);
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    private RideResponse response() {
        Instant now = Instant.parse("2026-09-30T00:00:00Z");
        return new RideResponse(5L, 1L, null, RideStatus.SEARCHING,
                new LocationResponse(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울시청"),
                new LocationResponse(new BigDecimal("37.4979"), new BigDecimal("127.0276"), null),
                now, now);
    }

    @Test
    void 호출을_생성하면_201과_Location을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.create(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/rides/5"))
                .andExpect(jsonPath("$.status").value("SEARCHING"))
                .andExpect(jsonPath("$.driverId").doesNotExist());
    }

    @Test
    void 위도_범위를_벗어나면_400을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickup":{"latitude":91,"longitude":126.9780},
                                 "destination":{"latitude":37.4979,"longitude":127.0276}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("pickup.latitude"));
    }

    @Test
    void 경도_범위를_벗어나면_400을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickup":{"latitude":37.5665,"longitude":126.9780},
                                 "destination":{"latitude":37.4979,"longitude":-180.5}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destination.longitude"));
    }

    @Test
    void 좌표가_없으면_400을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void 이미_진행_중인_호출이_있으면_409를_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.create(any(), any())).thenThrow(new BusinessException(ErrorCode.CONFLICT));

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void 승객이_아니면_403을_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.create(any(), any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/rides")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void X_User_Id_헤더가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rides/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 진행_중인_호출이_없으면_ride가_null이다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.getCurrent(any())).thenReturn(new CurrentRideResponse(null));

        mockMvc.perform(get("/api/v1/rides/current").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ride").isEmpty());
    }

    @Test
    void 진행_중인_호출을_조회한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.getCurrent(any())).thenReturn(new CurrentRideResponse(response()));

        mockMvc.perform(get("/api/v1/rides/current").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ride.id").value(5));
    }

    @Test
    void 호출_단건을_조회한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.get(any(), eq(5L))).thenReturn(response());

        mockMvc.perform(get("/api/v1/rides/5").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickup.address").value("서울시청"));
    }

    @Test
    void 다른_사용자의_호출을_조회하면_403을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.get(any(), eq(5L))).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/rides/5").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_호출을_조회하면_404를_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.get(any(), eq(5L))).thenThrow(new NotFoundException("호출을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/rides/5").header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 기사는_상태를_지정해_호출_목록을_조회한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.list(any(), eq(RideStatus.SEARCHING))).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/rides").param("status", "SEARCHING").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 상태를_생략하면_SEARCHING으로_조회한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.list(any(), eq(RideStatus.SEARCHING))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/rides").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 잘못된_상태값이면_400을_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);

        mockMvc.perform(get("/api/v1/rides").param("status", "UNKNOWN").header("X-User-Id", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 호출을_수락하면_ASSIGNED_상태를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        Instant now = Instant.parse("2026-09-30T00:00:00Z");
        when(rideService.accept(any(), eq(5L))).thenReturn(new RideResponse(5L, 1L, 10L, RideStatus.ASSIGNED,
                new LocationResponse(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울시청"),
                new LocationResponse(new BigDecimal("37.4979"), new BigDecimal("127.0276"), null),
                now, now));

        mockMvc.perform(post("/api/v1/rides/5/accept").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driverId").value(10));
    }

    @Test
    void 이미_수락된_호출이면_409를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.accept(any(), eq(5L))).thenThrow(new BusinessException(ErrorCode.CONFLICT));

        mockMvc.perform(post("/api/v1/rides/5/accept").header("X-User-Id", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void 낙관적_락_충돌이면_409를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.accept(any(), eq(5L)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Ride.class, 5L));

        mockMvc.perform(post("/api/v1/rides/5/accept").header("X-User-Id", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void 승객이_수락하면_403을_반환한다() throws Exception {
        login(1L, UserRole.PASSENGER);
        when(rideService.accept(any(), eq(5L))).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/rides/5/accept").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_호출을_수락하면_404를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.accept(any(), eq(5L))).thenThrow(new NotFoundException("호출을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/v1/rides/5/accept").header("X-User-Id", "2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 도착_처리하면_ARRIVED_상태를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.arrive(any(), eq(5L))).thenReturn(new RideStatusResponse(5L, RideStatus.ARRIVED, 2L));

        mockMvc.perform(post("/api/v1/rides/5/arrive").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(5))
                .andExpect(jsonPath("$.status").value("ARRIVED"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void 운행을_시작하면_IN_PROGRESS_상태를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.start(any(), eq(5L))).thenReturn(new RideStatusResponse(5L, RideStatus.IN_PROGRESS, 3L));

        mockMvc.perform(post("/api/v1/rides/5/start").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void 운행을_완료하면_COMPLETED_상태를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.complete(any(), eq(5L))).thenReturn(new RideStatusResponse(5L, RideStatus.COMPLETED, 4L));

        mockMvc.perform(post("/api/v1/rides/5/complete").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void 잘못된_상태_전환이면_409를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.start(any(), eq(5L))).thenThrow(new BusinessException(ErrorCode.CONFLICT));

        mockMvc.perform(post("/api/v1/rides/5/start").header("X-User-Id", "2"))
                .andExpect(status().isConflict());
    }

    @Test
    void 배정되지_않은_기사가_요청하면_403을_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.arrive(any(), eq(5L))).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/rides/5/arrive").header("X-User-Id", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 없는_호출의_운행_상태를_변경하면_404를_반환한다() throws Exception {
        login(2L, UserRole.DRIVER);
        when(rideService.complete(any(), eq(5L))).thenThrow(new NotFoundException("호출을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/v1/rides/5/complete").header("X-User-Id", "2"))
                .andExpect(status().isNotFound());
    }
}
