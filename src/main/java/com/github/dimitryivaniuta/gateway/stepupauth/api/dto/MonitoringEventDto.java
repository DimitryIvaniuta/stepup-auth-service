package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import java.time.Instant;

/** Monitoring event projection (payload is JSON in string form). */
public record MonitoringEventDto(
    long id,
    String type,
    Instant createdAt,
    String payloadJson
) { }
