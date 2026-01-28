package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.util.UUID;

/**
 * Login response.
 */
public record LoginResponse(
        UUID userId, String token) {
}
