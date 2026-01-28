package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Authorization request for risky action.
 */
public record AuthorizeTransactionRequest(
        @NotBlank String actionType,
        @NotNull @DecimalMin("0.00") BigDecimal amount) {
}
