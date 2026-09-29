package kim.autoever.taxi.vehicle.controller;

import jakarta.validation.Valid;
import kim.autoever.taxi.user.auth.CurrentUser;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.vehicle.dto.VehicleRequest;
import kim.autoever.taxi.vehicle.dto.VehicleResponse;
import kim.autoever.taxi.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/drivers/me/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@CurrentUser LoginUser loginUser,
                                                  @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.create(loginUser, request);
        return ResponseEntity.created(URI.create("/api/v1/drivers/me/vehicle")).body(response);
    }

    @GetMapping
    public VehicleResponse get(@CurrentUser LoginUser loginUser) {
        return vehicleService.get(loginUser);
    }

    @PutMapping
    public VehicleResponse update(@CurrentUser LoginUser loginUser,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(loginUser, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@CurrentUser LoginUser loginUser) {
        vehicleService.delete(loginUser);
        return ResponseEntity.noContent().build();
    }
}