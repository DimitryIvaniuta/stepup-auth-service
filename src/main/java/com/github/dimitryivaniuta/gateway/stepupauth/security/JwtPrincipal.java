package com.github.dimitryivaniuta.gateway.stepupauth.security;

import java.util.UUID;

/** Minimal authenticated principal extracted from JWT. */
public record JwtPrincipal(UUID userId, String username) { }
