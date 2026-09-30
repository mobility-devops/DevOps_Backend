package kim.autoever.taxi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공통 오류 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "오류 코드", example = "CONFLICT")
        String code,
        @Schema(description = "오류 메시지", example = "요청이 현재 상태와 충돌합니다.")
        String message,
        @Schema(description = "요청 추적 ID (X-Request-Id 헤더와 동일)", example = "req_1a2b3c4d5e6f")
        String requestId,
        @Schema(description = "필드별 검증 오류 (입력값 오류일 때만 포함)")
        List<FieldError> errors
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(String code, String message, String requestId) {
        return new ErrorResponse(code, message, requestId, null);
    }

    public static ErrorResponse of(String code, String message, String requestId, List<FieldError> errors) {
        return new ErrorResponse(code, message, requestId, errors);
    }
}