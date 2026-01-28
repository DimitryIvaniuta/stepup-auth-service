package com.github.dimitryivaniuta.gateway.stepupauth.api;

import org.springframework.http.HttpStatus;

/**
 * Exception mapped to HTTP status.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
