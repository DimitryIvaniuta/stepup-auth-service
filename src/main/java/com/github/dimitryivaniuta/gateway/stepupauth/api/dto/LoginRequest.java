package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request.
 */
public record LoginRequest(
        @NotBlank String username, @NotBlank String password) {
}
