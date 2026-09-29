package kim.autoever.taxi.vehicle.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.vehicle.domain.Vehicle;
import kim.autoever.taxi.vehicle.dto.VehicleRequest;
import kim.autoever.taxi.vehicle.dto.VehicleResponse;
import kim.autoever.taxi.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverService driverService;

    @Transactional
    public VehicleResponse create(LoginUser loginUser, VehicleRequest request) {
        Driver driver = driverService.getOrCreate(loginUser);
        if (vehicleRepository.existsByDriverId(driver.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 차량이 있습니다.");
        }
        String plateNumber = normalize(request.plateNumber());
        if (vehicleRepository.existsByPlateNumber(plateNumber)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 차량 번호입니다.");
        }
        Vehicle vehicle = vehicleRepository.save(Vehicle.create(driver, plateNumber, request.model().trim()));
        return VehicleResponse.from(vehicle);
    }

    public VehicleResponse get(LoginUser loginUser) {
        loginUser.requireRole(UserRole.DRIVER);
        return VehicleResponse.from(findVehicle(loginUser.id()));
    }

    @Transactional
    public VehicleResponse update(LoginUser loginUser, VehicleRequest request) {
        loginUser.requireRole(UserRole.DRIVER);
        Vehicle vehicle = findVehicle(loginUser.id());
        String plateNumber = normalize(request.plateNumber());
        if (vehicleRepository.existsByPlateNumberAndIdNot(plateNumber, vehicle.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 차량 번호입니다.");
        }
        vehicle.update(plateNumber, request.model().trim());
        vehicleRepository.flush();
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(LoginUser loginUser) {
        loginUser.requireRole(UserRole.DRIVER);
        vehicleRepository.delete(findVehicle(loginUser.id()));
    }

    private Vehicle findVehicle(Long userId) {
        return vehicleRepository.findByDriverUserId(userId)
                .orElseThrow(() -> new NotFoundException("등록된 차량이 없습니다."));
    }

    private String normalize(String plateNumber) {
        return plateNumber.replaceAll("\\s+", "");
    }
}