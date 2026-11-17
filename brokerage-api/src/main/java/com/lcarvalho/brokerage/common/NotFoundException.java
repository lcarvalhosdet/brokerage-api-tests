package com.lcarvalho.brokerage.common;

/** A resource named in the request path does not exist (HTTP 404). */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
