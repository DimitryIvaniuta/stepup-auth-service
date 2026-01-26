package com.github.dimitryivaniuta.gateway.stepupauth.config.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Transactional outbox publisher settings.
 */
@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        boolean enabled,
        @Min(1) int batchSize,
        @NotNull Duration publishInterval
) { }
