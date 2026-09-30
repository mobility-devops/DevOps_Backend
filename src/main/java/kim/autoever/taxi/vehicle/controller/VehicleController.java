package kim.autoever.taxi.vehicle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "차량")
@RestController
@RequestMapping("/api/v1/drivers/me/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "차량 등록", description = "DRIVER 전용. 기사 1명당 차량 1대만 등록할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "409", description = "이미 차량이 있거나 차량 번호 중복")
    })
    @PostMapping
    public ResponseEntity<VehicleResponse> create(@CurrentUser LoginUser loginUser,
                                                  @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.create(loginUser, request);
        return ResponseEntity.created(URI.create("/api/v1/drivers/me/vehicle")).body(response);
    }

    @Operation(summary = "내 차량 조회", description = "DRIVER 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "404", description = "등록된 차량 없음")
    })
    @GetMapping
    public VehicleResponse get(@CurrentUser LoginUser loginUser) {
        return vehicleService.get(loginUser);
    }

    @Operation(summary = "차량 수정", description = "DRIVER 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "404", description = "등록된 차량 없음"),
            @ApiResponse(responseCode = "409", description = "차량 번호 중복")
    })
    @PutMapping
    public VehicleResponse update(@CurrentUser LoginUser loginUser,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(loginUser, request);
    }

    @Operation(summary = "차량 삭제", description = "DRIVER 전용.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "404", description = "등록된 차량 없음")
    })
    @DeleteMapping
    public ResponseEntity<Void> delete(@CurrentUser LoginUser loginUser) {
        vehicleService.delete(loginUser);
        return ResponseEntity.noContent().build();
    }
}
