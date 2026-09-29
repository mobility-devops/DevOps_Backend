package kim.autoever.taxi.driver.controller;

import jakarta.validation.Valid;
import kim.autoever.taxi.driver.dto.DriverAvailabilityRequest;
import kim.autoever.taxi.driver.dto.DriverResponse;
import kim.autoever.taxi.driver.service.DriverService;
import kim.autoever.taxi.user.auth.CurrentUser;
import kim.autoever.taxi.user.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers/me")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public DriverResponse getMe(@CurrentUser LoginUser loginUser) {
        return driverService.getMe(loginUser);
    }

    @PutMapping("/availability")
    public DriverResponse changeAvailability(@CurrentUser LoginUser loginUser,
                                             @Valid @RequestBody DriverAvailabilityRequest request) {
        return driverService.changeAvailability(loginUser, request);
    }
}