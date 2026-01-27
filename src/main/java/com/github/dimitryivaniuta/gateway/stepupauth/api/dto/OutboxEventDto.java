package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Outbox row projection for admin operations. */
public record OutboxEventDto(
    long id,
    UUID aggregateId,
    String type,
    String payloadJson,
    String status,
    int attempts,
    Instant createdAt,
    Instant nextAttemptAt,
    Instant publishedAt,
    String lastError
) { }
