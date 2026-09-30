package kim.autoever.taxi.ride.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.domain.DriverAvailability;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.ride.domain.ActiveAssignment;
import kim.autoever.taxi.ride.domain.ActivePassengerRide;
import kim.autoever.taxi.ride.domain.Location;
import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.LocationRequest;
import kim.autoever.taxi.ride.dto.RideCreateRequest;
import kim.autoever.taxi.ride.dto.RideResponse;
import kim.autoever.taxi.ride.repository.ActiveAssignmentRepository;
import kim.autoever.taxi.ride.repository.ActivePassengerRideRepository;
import kim.autoever.taxi.ride.repository.RideRepository;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private ActivePassengerRideRepository activePassengerRideRepository;

    @Mock
    private ActiveAssignmentRepository activeAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private RideService rideService;

    private final LoginUser passengerLogin = new LoginUser(1L, UserRole.PASSENGER);
    private final LoginUser driverLogin = new LoginUser(2L, UserRole.DRIVER);
    private User passenger;
    private Driver driver;

    @BeforeEach
    void setUp() {
        passenger = User.create("승객", "01011112222", UserRole.PASSENGER);
        ReflectionTestUtils.setField(passenger, "id", 1L);
        User driverUser = User.create("기사", "01033334444", UserRole.DRIVER);
        ReflectionTestUtils.setField(driverUser, "id", 2L);
        driver = Driver.create(driverUser);
        ReflectionTestUtils.setField(driver, "id", 10L);
    }

    private RideCreateRequest request() {
        return new RideCreateRequest(
                new LocationRequest(new BigDecimal("37.5665"), new BigDecimal("126.9780"), " 서울시청 "),
                new LocationRequest(new BigDecimal("37.4979"), new BigDecimal("127.0276"), null));
    }

    private Ride ride(long id) {
        Ride ride = Ride.create(passenger,
                Location.of(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울시청"),
                Location.of(new BigDecimal("37.4979"), new BigDecimal("127.0276"), null));
        ReflectionTestUtils.setField(ride, "id", id);
        return ride;
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    void 호출을_생성하면_SEARCHING_상태이고_활성_호출이_저장된다() {
        when(activePassengerRideRepository.existsByPassengerId(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.create(passengerLogin, request());

        assertThat(response.status()).isEqualTo(RideStatus.SEARCHING);
        assertThat(response.passengerId()).isEqualTo(1L);
        assertThat(response.driverId()).isNull();
        assertThat(response.pickup().address()).isEqualTo("서울시청");
        assertThat(response.destination().address()).isNull();
        verify(activePassengerRideRepository).save(any(ActivePassengerRide.class));
    }

    @Test
    void 진행_중인_호출이_있으면_409_예외가_발생한다() {
        when(activePassengerRideRepository.existsByPassengerId(1L)).thenReturn(true);

        assertError(() -> rideService.create(passengerLogin, request()), ErrorCode.CONFLICT);
        verify(rideRepository, never()).save(any());
    }

    @Test
    void 기사가_호출을_생성하면_403_예외가_발생한다() {
        assertError(() -> rideService.create(driverLogin, request()), ErrorCode.FORBIDDEN);
        verify(rideRepository, never()).save(any());
    }

    @Test
    void 진행_중인_호출이_있으면_현재_호출을_반환한다() {
        when(activePassengerRideRepository.findByPassengerId(1L))
                .thenReturn(Optional.of(ActivePassengerRide.create(passenger, ride(5L))));

        CurrentRideResponse response = rideService.getCurrent(passengerLogin);

        assertThat(response.ride()).isNotNull();
        assertThat(response.ride().id()).isEqualTo(5L);
    }

    @Test
    void 진행_중인_호출이_없으면_ride가_null이다() {
        when(activePassengerRideRepository.findByPassengerId(1L)).thenReturn(Optional.empty());

        assertThat(rideService.getCurrent(passengerLogin).ride()).isNull();
    }

    @Test
    void 기사가_현재_호출을_조회하면_403_예외가_발생한다() {
        assertError(() -> rideService.getCurrent(driverLogin), ErrorCode.FORBIDDEN);
    }

    @Test
    void 본인_호출을_조회한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));

        assertThat(rideService.get(passengerLogin, 5L).id()).isEqualTo(5L);
    }

    @Test
    void 배정된_기사는_호출을_조회할_수_있다() {
        Ride ride = ride(5L);
        ReflectionTestUtils.setField(ride, "driver", driver);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride));

        RideResponse response = rideService.get(driverLogin, 5L);

        assertThat(response.driverId()).isEqualTo(10L);
    }

    @Test
    void 다른_사용자의_호출을_조회하면_403_예외가_발생한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));

        assertError(() -> rideService.get(new LoginUser(99L, UserRole.PASSENGER), 5L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 배정되지_않은_기사가_호출을_조회하면_403_예외가_발생한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));

        assertError(() -> rideService.get(driverLogin, 5L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_호출을_조회하면_404_예외가_발생한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.get(passengerLogin, 5L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 기사는_상태별_호출_목록을_조회한다() {
        when(rideRepository.findByStatusOrderByIdAsc(RideStatus.SEARCHING))
                .thenReturn(List.of(ride(5L), ride(6L)));

        List<RideResponse> responses = rideService.list(driverLogin, RideStatus.SEARCHING);

        assertThat(responses).extracting(RideResponse::id).containsExactly(5L, 6L);
    }

    @Test
    void 승객이_호출_목록을_조회하면_403_예외가_발생한다() {
        assertError(() -> rideService.list(passengerLogin, RideStatus.SEARCHING), ErrorCode.FORBIDDEN);
        verify(rideRepository, never()).findByStatusOrderByIdAsc(any());
    }

    @Test
    void 기사가_호출을_수락하면_ASSIGNED_상태가_되고_배정_정보가_저장된다() {
        driver.changeAvailability(DriverAvailability.ONLINE);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(activeAssignmentRepository.existsByDriverId(10L)).thenReturn(false);

        RideResponse response = rideService.accept(driverLogin, 5L);

        assertThat(response.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(response.driverId()).isEqualTo(10L);
        verify(activeAssignmentRepository).save(any(ActiveAssignment.class));
        verify(rideRepository).flush();
    }

    @Test
    void OFFLINE_기사가_수락하면_409_예외가_발생한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);

        assertError(() -> rideService.accept(driverLogin, 5L), ErrorCode.CONFLICT);
        verify(activeAssignmentRepository, never()).save(any());
    }

    @Test
    void 이미_운행_중인_기사가_수락하면_409_예외가_발생한다() {
        driver.changeAvailability(DriverAvailability.ONLINE);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride(5L)));
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(activeAssignmentRepository.existsByDriverId(10L)).thenReturn(true);

        assertError(() -> rideService.accept(driverLogin, 5L), ErrorCode.CONFLICT);
        verify(activeAssignmentRepository, never()).save(any());
    }

    @Test
    void 이미_수락된_호출을_수락하면_409_예외가_발생한다() {
        driver.changeAvailability(DriverAvailability.ONLINE);
        Ride ride = ride(5L);
        ride.accept(driver);
        when(rideRepository.findById(5L)).thenReturn(Optional.of(ride));
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(activeAssignmentRepository.existsByDriverId(10L)).thenReturn(false);

        assertError(() -> rideService.accept(driverLogin, 5L), ErrorCode.CONFLICT);
        verify(activeAssignmentRepository, never()).save(any());
    }

    @Test
    void 승객이_수락하면_403_예외가_발생한다() {
        assertError(() -> rideService.accept(passengerLogin, 5L), ErrorCode.FORBIDDEN);
        verify(rideRepository, never()).findById(any());
    }

    @Test
    void 없는_호출을_수락하면_404_예외가_발생한다() {
        when(rideRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.accept(driverLogin, 5L))
                .isInstanceOf(NotFoundException.class);
    }
}
