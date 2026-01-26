package com.github.dimitryivaniuta.gateway.stepupauth.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application Kafka-related properties (app-owned, not Spring Kafka ones).
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(Topics topics) {

    public record Topics(
            @NotBlank String monitoring
    ) { }
}
