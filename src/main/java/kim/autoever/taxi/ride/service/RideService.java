package kim.autoever.taxi.ride.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.ride.domain.ActivePassengerRide;
import kim.autoever.taxi.ride.domain.Ride;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.RideCreateRequest;
import kim.autoever.taxi.ride.dto.RideResponse;
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
    private final UserRepository userRepository;

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
