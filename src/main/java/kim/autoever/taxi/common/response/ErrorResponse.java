package kim.autoever.taxi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String requestId,
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