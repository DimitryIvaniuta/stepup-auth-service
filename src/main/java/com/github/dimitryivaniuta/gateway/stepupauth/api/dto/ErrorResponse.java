package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.time.Instant;

/**
 * API error response.
 */
public record ErrorResponse(
        Instant timestamp, int status, String error, String message, String path) {
}
