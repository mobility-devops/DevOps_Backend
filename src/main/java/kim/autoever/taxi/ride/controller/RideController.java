package kim.autoever.taxi.ride.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kim.autoever.taxi.ride.domain.RideStatus;
import kim.autoever.taxi.ride.dto.CurrentRideResponse;
import kim.autoever.taxi.ride.dto.RideCreateRequest;
import kim.autoever.taxi.ride.dto.RideResponse;
import kim.autoever.taxi.ride.dto.RideStatusResponse;
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

@Tag(name = "호출")
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @Operation(summary = "호출 생성", description = "PASSENGER 전용. 출발지·목적지 좌표로 호출하며 초기 상태는 SEARCHING 입니다. 승객당 진행 중인 호출은 1개입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "호출 생성",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류 (좌표 범위 등)"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "PASSENGER 권한 필요"),
            @ApiResponse(responseCode = "409", description = "진행 중인 호출이 이미 있음")
    })
    @PostMapping
    public ResponseEntity<RideResponse> create(@CurrentUser LoginUser loginUser,
                                               @Valid @RequestBody RideCreateRequest request) {
        RideResponse response = rideService.create(loginUser, request);
        return ResponseEntity.created(URI.create("/api/v1/rides/" + response.id())).body(response);
    }

    @Operation(summary = "내 진행 중 호출 조회", description = "PASSENGER 전용. 진행 중인 호출이 없으면 `{\"ride\": null}` 을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CurrentRideResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "PASSENGER 권한 필요")
    })
    @GetMapping("/current")
    public CurrentRideResponse getCurrent(@CurrentUser LoginUser loginUser) {
        return rideService.getCurrent(loginUser);
    }

    @Operation(summary = "호출 단건 조회", description = "호출한 승객 또는 배정된 기사만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출")
    })
    @GetMapping("/{rideId}")
    public RideResponse get(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.get(loginUser, rideId);
    }

    @Operation(summary = "호출 수락", description = "DRIVER 전용. ONLINE 기사가 SEARCHING 호출을 수락하면 ASSIGNED 가 됩니다. 중복 수락은 낙관적 락으로 막습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수락 성공",
                    content = @Content(schema = @Schema(implementation = RideResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출"),
            @ApiResponse(responseCode = "409", description = "OFFLINE 기사, 운행 중인 기사, 이미 수락·취소된 호출, 동시 수락 충돌")
    })
    @PostMapping("/{rideId}/accept")
    public RideResponse accept(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.accept(loginUser, rideId);
    }

    @Operation(summary = "출발지 도착", description = "배정된 기사 전용. ASSIGNED → ARRIVED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = RideStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "배정된 기사만 가능"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출"),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전환")
    })
    @PostMapping("/{rideId}/arrive")
    public RideStatusResponse arrive(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.arrive(loginUser, rideId);
    }

    @Operation(summary = "운행 시작", description = "배정된 기사 전용. ARRIVED → IN_PROGRESS")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = RideStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "배정된 기사만 가능"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출"),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전환")
    })
    @PostMapping("/{rideId}/start")
    public RideStatusResponse start(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.start(loginUser, rideId);
    }

    @Operation(summary = "운행 완료", description = "배정된 기사 전용. IN_PROGRESS → COMPLETED. 승객·기사의 활성 정보가 해제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = RideStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "배정된 기사만 가능"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출"),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전환")
    })
    @PostMapping("/{rideId}/complete")
    public RideStatusResponse complete(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.complete(loginUser, rideId);
    }

    @Operation(summary = "호출 취소", description = "호출한 승객 전용. SEARCHING, ASSIGNED, ARRIVED 에서만 CANCELLED 로 변경할 수 있으며 활성 정보가 해제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공",
                    content = @Content(schema = @Schema(implementation = RideStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "호출한 승객만 가능"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 호출"),
            @ApiResponse(responseCode = "409", description = "취소할 수 없는 상태 (IN_PROGRESS, COMPLETED, CANCELLED)")
    })
    @PostMapping("/{rideId}/cancel")
    public RideStatusResponse cancel(@CurrentUser LoginUser loginUser, @PathVariable Long rideId) {
        return rideService.cancel(loginUser, rideId);
    }

    @Operation(summary = "호출 목록 조회", description = "DRIVER 전용. 상태별로 조회하며 status 를 생략하면 SEARCHING 입니다. 기사는 이 목록에서 호출을 골라 수락합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class)))),
            @ApiResponse(responseCode = "400", description = "잘못된 status 값"),
            @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "DRIVER 권한 필요")
    })
    @GetMapping
    public List<RideResponse> list(@CurrentUser LoginUser loginUser,
                                   @Parameter(description = "조회할 호출 상태", example = "SEARCHING")
                                   @RequestParam(defaultValue = "SEARCHING") RideStatus status) {
        return rideService.list(loginUser, status);
    }
}
