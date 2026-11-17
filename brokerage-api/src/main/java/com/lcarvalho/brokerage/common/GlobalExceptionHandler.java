package com.lcarvalho.brokerage.common;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns every error into the same {@link ApiError} body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation failed on the request body (missing, blank or out-of-range fields). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException ex) {
        List<ApiError.FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldViolation::field))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "The request has invalid fields", details));
    }

    /** Invalid JSON, an unknown enum value, a malformed UUID or a fractional quantity. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("MALFORMED_REQUEST", "The request body could not be read"));
    }

    /** A path variable with the wrong format, such as an id that is not a UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("INVALID_PARAMETER", "Invalid value for parameter '" + ex.getName() + "'"));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> businessRule(BusinessRuleException ex) {
        return ResponseEntity.unprocessableEntity().body(ApiError.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception ex) {
        // Errors raised by Spring itself (unknown path, wrong HTTP method, wrong content type)
        // keep their own status code
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            String detail = springError.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(ApiError.of("HTTP_" + status.value(), detail != null ? detail : ex.getMessage()));
        }
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Unexpected error"));
    }
}
