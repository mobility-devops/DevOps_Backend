package kim.autoever.taxi.ride.controller;

import jakarta.validation.Valid;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.RideCreateRequest;
import kim.autoever.taxi.ride.dto.RideResponse;
import kim.autoever.taxi.ride.service.RideService;
import kim.autoever.taxi.user.auth.CurrentUser;
import kim.autoever.taxi.user.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    public ResponseEntity<RideResponse> create(@CurrentUser LoginUser loginUser,
                                               @Valid @RequestBody RideCreateRequest request) {
        RideResponse response = rideService.create(loginUser, request);
        return ResponseEntity.created(URI.create("/api/v1/rides/" + response.id())).body(response);
    }

    @GetMapping("/current")
    public CurrentRideResponse getCurrent(@CurrentUser LoginUser loginUser) {
        return rideService.getCurrent(loginUser);
    }

    @GetMapping("/{rideId}")
    public RideResponse get(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.get(loginUser, rideId);
    }

    @GetMapping
    public List<RideResponse> list(@CurrentUser LoginUser loginUser,
                                   @RequestParam(defaultValue = "SEARCHING") RideStatus status) {
        return rideService.list(loginUser, status);
    }
}
