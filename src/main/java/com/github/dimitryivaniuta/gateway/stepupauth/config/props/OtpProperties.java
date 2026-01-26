package com.github.dimitryivaniuta.gateway.stepupauth.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * OTP settings.
 *
 * <p>Note: OTP TTL and attempts are enforced by {@code OtpService} using Redis.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        Duration ttl,
        int maxAttempts,
        boolean devPreview
) {
}
