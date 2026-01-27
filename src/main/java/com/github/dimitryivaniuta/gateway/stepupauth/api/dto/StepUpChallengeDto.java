package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Step-up challenge projection for APIs. */
public record StepUpChallengeDto(
    UUID id,
    UUID userId,
    UUID decisionId,
    String status,
    int attempts,
    Instant createdAt,
    Instant verifiedAt
) { }
