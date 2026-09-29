package kim.autoever.taxi.vehicle.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.vehicle.domain.Vehicle;
import kim.autoever.taxi.vehicle.dto.VehicleRequest;
import kim.autoever.taxi.vehicle.dto.VehicleResponse;
import kim.autoever.taxi.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private VehicleService vehicleService;

    private final LoginUser driverLogin = new LoginUser(1L, UserRole.DRIVER);
    private Driver driver;

    @BeforeEach
    void setUp() {
        User user = User.create("홍길동", "01012345678", UserRole.DRIVER);
        ReflectionTestUtils.setField(user, "id", 1L);
        driver = Driver.create(user);
        ReflectionTestUtils.setField(driver, "id", 10L);
    }

    private Vehicle vehicleWithId(long id) {
        Vehicle vehicle = Vehicle.create(driver, "12가3456", "쏘나타");
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void 차량을_등록한다_공백은_제거한다() {
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(vehicleRepository.existsByDriverId(10L)).thenReturn(false);
        when(vehicleRepository.existsByPlateNumber("12가3456")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse response = vehicleService.create(driverLogin, new VehicleRequest(" 12가 3456 ", " 쏘나타 "));

        assertThat(response.plateNumber()).isEqualTo("12가3456");
        assertThat(response.model()).isEqualTo("쏘나타");
        assertThat(response.driverId()).isEqualTo(10L);
    }

    @Test
    void 이미_차량이_있으면_409_예외가_발생한다() {
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(vehicleRepository.existsByDriverId(10L)).thenReturn(true);

        assertConflict(() -> vehicleService.create(driverLogin, new VehicleRequest("12가3456", "쏘나타")));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void 차량_번호가_중복이면_409_예외가_발생한다() {
        when(driverService.getOrCreate(driverLogin)).thenReturn(driver);
        when(vehicleRepository.existsByDriverId(10L)).thenReturn(false);
        when(vehicleRepository.existsByPlateNumber("12가3456")).thenReturn(true);

        assertConflict(() -> vehicleService.create(driverLogin, new VehicleRequest("12가3456", "쏘나타")));
    }

    @Test
    void 차량을_조회한다() {
        when(vehicleRepository.findByDriverUserId(1L)).thenReturn(Optional.of(vehicleWithId(5L)));

        VehicleResponse response = vehicleService.get(driverLogin);

        assertThat(response.plateNumber()).isEqualTo("12가3456");
    }

    @Test
    void 등록된_차량이_없으면_404_예외가_발생한다() {
        when(vehicleRepository.findByDriverUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.get(driverLogin))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 승객이_차량을_조회하면_403_예외가_발생한다() {
        LoginUser passenger = new LoginUser(2L, UserRole.PASSENGER);

        assertThatThrownBy(() -> vehicleService.get(passenger))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 차량을_수정한다() {
        when(vehicleRepository.findByDriverUserId(1L)).thenReturn(Optional.of(vehicleWithId(5L)));
        when(vehicleRepository.existsByPlateNumberAndIdNot("34나5678", 5L)).thenReturn(false);

        VehicleResponse response = vehicleService.update(driverLogin, new VehicleRequest("34나5678", "그랜저"));

        assertThat(response.plateNumber()).isEqualTo("34나5678");
        assertThat(response.model()).isEqualTo("그랜저");
    }

    @Test
    void 다른_차량의_번호로_수정하면_409_예외가_발생한다() {
        when(vehicleRepository.findByDriverUserId(1L)).thenReturn(Optional.of(vehicleWithId(5L)));
        when(vehicleRepository.existsByPlateNumberAndIdNot("34나5678", 5L)).thenReturn(true);

        assertConflict(() -> vehicleService.update(driverLogin, new VehicleRequest("34나5678", "그랜저")));
    }

    @Test
    void 차량을_삭제한다() {
        Vehicle vehicle = vehicleWithId(5L);
        when(vehicleRepository.findByDriverUserId(1L)).thenReturn(Optional.of(vehicle));

        vehicleService.delete(driverLogin);

        verify(vehicleRepository).delete(vehicle);
    }
}