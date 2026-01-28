package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Risk decision projection for APIs.
 */
public record RiskDecisionDto(
        UUID id,
        UUID userId,
        String actionType,
        BigDecimal amount,
        String country,
        int riskScore,
        String riskLevel,
        String decision,
        Instant createdAt,
        UUID stepUpChallengeId
) {
}
