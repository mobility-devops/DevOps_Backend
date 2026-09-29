package kim.autoever.taxi.common.exception;

import jakarta.validation.ConstraintViolationException;
import kim.autoever.taxi.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return build(errorCode, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return build(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return build(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode,
                                                String message,
                                                List<ErrorResponse.FieldError> errors) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_KEY);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.name(), message, requestId, errors));
    }
}