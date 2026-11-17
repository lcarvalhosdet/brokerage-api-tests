package com.lcarvalho.brokerage.common;

import java.util.List;

/** Error body returned by every endpoint, so clients and tests can rely on one format. */
public record ApiError(String code, String message, List<FieldViolation> details) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
