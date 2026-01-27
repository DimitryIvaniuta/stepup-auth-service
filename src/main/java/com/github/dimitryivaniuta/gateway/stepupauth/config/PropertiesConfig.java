package com.github.dimitryivaniuta.gateway.stepupauth.config;

import com.github.dimitryivaniuta.gateway.stepupauth.config.props.AppKafkaProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.OtpProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.OutboxProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables strongly typed configuration properties.
 */
@Configuration
@EnableConfigurationProperties({OutboxProperties.class,
        AppKafkaProperties.class,
        OtpProperties.class,
        OutboxProperties.class,
        KafkaProperties.class
})
public class PropertiesConfig {
}
