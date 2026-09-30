package kim.autoever.taxi.driver.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.domain.DriverAvailability;
import kim.autoever.taxi.driver.dto.DriverAvailabilityRequest;
import kim.autoever.taxi.driver.dto.DriverResponse;
import kim.autoever.taxi.driver.repository.DriverRepository;
import kim.autoever.taxi.ride.repository.ActiveAssignmentRepository;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActiveAssignmentRepository activeAssignmentRepository;

    @InjectMocks
    private DriverService driverService;

    private User driverUser() {
        User user = User.create("홍길동", "01012345678", UserRole.DRIVER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void 기사_정보가_없으면_OFFLINE으로_생성한다() {
        LoginUser loginUser = new LoginUser(1L, UserRole.DRIVER);
        when(driverRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(driverUser()));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.getMe(loginUser);

        assertThat(response.availability()).isEqualTo(DriverAvailability.OFFLINE);
        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    void 기사_상태를_변경한다() {
        LoginUser loginUser = new LoginUser(1L, UserRole.DRIVER);
        Driver driver = Driver.create(driverUser());
        when(driverRepository.findByUserId(1L)).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.changeAvailability(
                loginUser, new DriverAvailabilityRequest(DriverAvailability.ONLINE));

        assertThat(response.availability()).isEqualTo(DriverAvailability.ONLINE);
        verify(driverRepository).flush();
    }

    @Test
    void 운행_중인_기사가_OFFLINE으로_변경하면_409_예외가_발생한다() {
        LoginUser loginUser = new LoginUser(1L, UserRole.DRIVER);
        Driver driver = Driver.create(driverUser());
        ReflectionTestUtils.setField(driver, "id", 10L);
        driver.changeAvailability(DriverAvailability.ONLINE);
        when(driverRepository.findByUserId(1L)).thenReturn(Optional.of(driver));
        when(activeAssignmentRepository.existsByDriverId(10L)).thenReturn(true);

        assertThatThrownBy(() -> driverService.changeAvailability(
                loginUser, new DriverAvailabilityRequest(DriverAvailability.OFFLINE)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        assertThat(driver.getAvailability()).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void 운행_중이_아닌_기사는_OFFLINE으로_변경할_수_있다() {
        LoginUser loginUser = new LoginUser(1L, UserRole.DRIVER);
        Driver driver = Driver.create(driverUser());
        ReflectionTestUtils.setField(driver, "id", 10L);
        driver.changeAvailability(DriverAvailability.ONLINE);
        when(driverRepository.findByUserId(1L)).thenReturn(Optional.of(driver));
        when(activeAssignmentRepository.existsByDriverId(10L)).thenReturn(false);

        DriverResponse response = driverService.changeAvailability(
                loginUser, new DriverAvailabilityRequest(DriverAvailability.OFFLINE));

        assertThat(response.availability()).isEqualTo(DriverAvailability.OFFLINE);
    }

    @Test
    void 승객이_기사_정보를_조회하면_403_예외가_발생한다() {
        LoginUser passenger = new LoginUser(2L, UserRole.PASSENGER);

        assertThatThrownBy(() -> driverService.getMe(passenger))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}