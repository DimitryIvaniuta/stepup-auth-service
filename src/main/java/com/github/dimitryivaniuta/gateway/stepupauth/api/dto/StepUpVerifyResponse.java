package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.util.UUID;

/**
 * OTP verify response.
 */
public record StepUpVerifyResponse(
        String status,
        UUID decisionId
) {
}
