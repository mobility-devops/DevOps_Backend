package kim.autoever.taxi.driver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "기사")
@RestController
@RequestMapping("/api/v1/drivers/me")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @Operation(summary = "내 기사 정보 조회", description = "DRIVER 전용. 기사 정보가 없으면 OFFLINE 상태로 자동 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    })
    @GetMapping
    public DriverResponse getMe(@CurrentUser LoginUser loginUser) {
        return driverService.getMe(loginUser);
    }

    @Operation(summary = "기사 상태 변경", description = "ONLINE/OFFLINE 으로 변경합니다. 운행 중인 기사는 OFFLINE 으로 변경할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = DriverResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "409", description = "운행 중에는 OFFLINE 으로 변경 불가")
    })
    @PutMapping("/availability")
    public DriverResponse changeAvailability(@CurrentUser LoginUser loginUser,
                                             @Valid @RequestBody DriverAvailabilityRequest request) {
        return driverService.changeAvailability(loginUser, request);
    }
}
