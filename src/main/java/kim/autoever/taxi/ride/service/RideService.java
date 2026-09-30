package kim.autoever.taxi.ride.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.driver.domain.DriverAvailability;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.ride.domain.ActiveAssignment;
import kim.autoever.taxi.ride.domain.ActivePassengerRide;
import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.RideCreateRequest;
import kim.autoever.taxi.ride.dto.RideResponse;
import kim.autoever.taxi.ride.dto.RideStatusResponse;
import kim.autoever.taxi.ride.repository.ActiveAssignmentRepository;
import kim.autoever.taxi.ride.repository.ActivePassengerRideRepository;
import kim.autoever.taxi.ride.repository.RideRepository;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideService {

    private final RideRepository rideRepository;
    private final ActivePassengerRideRepository activePassengerRideRepository;
    private final ActiveAssignmentRepository activeAssignmentRepository;
    private final UserRepository userRepository;
    private final DriverService driverService;

    /**
     * 기사가 호출을 수락한다. 동시 수락은 rides.version(낙관적 락)과
     * active_assignments UNIQUE 제약으로 막고, 둘 다 409 로 응답한다.
     */
    @Transactional
    public RideResponse accept(LoginUser loginUser, Long rideId) {
        loginUser.requireRole(UserRole.DRIVER);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("호출을 찾을 수 없습니다."));
        Driver driver = driverService.getOrCreate(loginUser);
        if (driver.getAvailability() != DriverAvailability.ONLINE) {
            throw new BusinessException(ErrorCode.CONFLICT, "ONLINE 상태의 기사만 호출을 수락할 수 있습니다.");
        }
        if (activeAssignmentRepository.existsByDriverId(driver.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 진행 중인 운행이 있습니다.");
        }

        ride.accept(driver);
        activeAssignmentRepository.save(ActiveAssignment.create(driver, ride));
        rideRepository.flush();
        return RideResponse.from(ride);
    }

    @Transactional
    public RideStatusResponse arrive(LoginUser loginUser, Long rideId) {
        Ride ride = findAssignedRide(loginUser, rideId);
        ride.arrive();
        return flushAndRespond(ride);
    }

    @Transactional
    public RideStatusResponse start(LoginUser loginUser, Long rideId) {
        Ride ride = findAssignedRide(loginUser, rideId);
        ride.start();
        return flushAndRespond(ride);
    }

    /** 운행 완료 시 승객·기사의 활성 정보를 같은 트랜잭션에서 해제한다. */
    @Transactional
    public RideStatusResponse complete(LoginUser loginUser, Long rideId) {
        Ride ride = findAssignedRide(loginUser, rideId);
        ride.complete();
        activePassengerRideRepository.deleteByRideId(ride.getId());
        activeAssignmentRepository.deleteByRideId(ride.getId());
        return flushAndRespond(ride);
    }

    /**
     * 호출한 승객이 탑승 전까지 호출을 취소한다. 승객·기사의 활성 정보를 같은 트랜잭션에서 해제해
     * 승객은 다시 호출하고 배정됐던 기사는 다른 호출을 수락할 수 있다.
     */
    @Transactional
    public RideStatusResponse cancel(LoginUser loginUser, Long rideId) {
        loginUser.requireRole(UserRole.PASSENGER);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("호출을 찾을 수 없습니다."));
        if (!ride.isPassenger(loginUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "호출한 승객만 취소할 수 있습니다.");
        }
        ride.cancel();
        activePassengerRideRepository.deleteByRideId(ride.getId());
        activeAssignmentRepository.deleteByRideId(ride.getId());
        return flushAndRespond(ride);
    }

    private Ride findAssignedRide(LoginUser loginUser, Long rideId) {
        loginUser.requireRole(UserRole.DRIVER);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("호출을 찾을 수 없습니다."));
        if (!ride.isAssignedDriver(loginUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "배정된 기사만 운행 상태를 변경할 수 있습니다.");
        }
        return ride;
    }

    private RideStatusResponse flushAndRespond(Ride ride) {
        rideRepository.flush();
        return RideStatusResponse.from(ride);
    }

    @Transactional
    public RideResponse create(LoginUser loginUser, RideCreateRequest request) {
        loginUser.requireRole(UserRole.PASSENGER);
        if (activePassengerRideRepository.existsByPassengerId(loginUser.id())) {
            throw new BusinessException(ErrorCode.CONFLICT, "진행 중인 호출이 이미 있습니다.");
        }

        User passenger = userRepository.findById(loginUser.id())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        Ride ride = rideRepository.save(
                Ride.create(passenger, request.pickup().toLocation(), request.destination().toLocation()));
        // 동시 요청은 passenger_id UNIQUE 위반(DataIntegrityViolationException → 409)으로 막힌다.
        activePassengerRideRepository.save(ActivePassengerRide.create(passenger, ride));
        return RideResponse.from(ride);
    }

    public CurrentRideResponse getCurrent(LoginUser loginUser) {
        loginUser.requireRole(UserRole.PASSENGER);
        return activePassengerRideRepository.findByPassengerId(loginUser.id())
                .map(active -> new CurrentRideResponse(RideResponse.from(active.getRide())))
                .orElseGet(() -> new CurrentRideResponse(null));
    }

    public RideResponse get(LoginUser loginUser, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("호출을 찾을 수 없습니다."));
        if (!ride.isParticipant(loginUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 호출을 조회할 권한이 없습니다.");
        }
        return RideResponse.from(ride);
    }

    public List<RideResponse> list(LoginUser loginUser, RideStatus status) {
        loginUser.requireRole(UserRole.DRIVER);
        return rideRepository.findByStatusOrderByIdAsc(status).stream()
                .map(RideResponse::from)
                .toList();
    }
}
