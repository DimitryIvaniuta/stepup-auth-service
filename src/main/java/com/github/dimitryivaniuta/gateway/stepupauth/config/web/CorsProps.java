package com.github.dimitryivaniuta.gateway.stepupauth.config.web;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.web.cors")
public record CorsProps(
        List<String> allowedOrigins,
        boolean allowCredentials
) {
}