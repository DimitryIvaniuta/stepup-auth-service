package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.util.UUID;

/**
 * Authorization response.
 */
public record AuthorizeTransactionResponse(
        String decision, UUID decisionId, int riskScore, String riskLevel,
        UUID challengeId, String otpPreview) {
}
