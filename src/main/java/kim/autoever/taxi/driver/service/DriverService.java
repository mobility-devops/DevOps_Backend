package kim.autoever.taxi.driver.service;

import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.dto.DriverAvailabilityRequest;
import kim.autoever.taxi.driver.dto.DriverResponse;
import kim.autoever.taxi.driver.repository.DriverRepository;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    @Transactional
    public DriverResponse getMe(LoginUser loginUser) {
        return DriverResponse.from(getOrCreate(loginUser));
    }

    @Transactional
    public DriverResponse changeAvailability(LoginUser loginUser, DriverAvailabilityRequest request) {
        Driver driver = getOrCreate(loginUser);
        driver.changeAvailability(request.availability());
        driverRepository.flush();
        return DriverResponse.from(driver);
    }

    /**
     * 기사 정보가 없으면 OFFLINE 상태로 처음 생성한다. 차량 등록 등 다른 기능에서도 사용한다.
     */
    @Transactional
    public Driver getOrCreate(LoginUser loginUser) {
        loginUser.requireRole(UserRole.DRIVER);
        return driverRepository.findByUserId(loginUser.id())
                .orElseGet(() -> driverRepository.save(Driver.create(findUser(loginUser.id()))));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}