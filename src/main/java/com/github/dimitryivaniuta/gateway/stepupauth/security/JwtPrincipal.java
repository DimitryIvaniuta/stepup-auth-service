package com.github.dimitryivaniuta.gateway.stepupauth.security;

import java.util.UUID;

/**
 * Authenticated principal extracted from JWT.
 *
 * @param userId   application user id
 * @param username username
 * @param roles    role names from JWT claim (e.g. {@code USER}, {@code ADMIN})
 */
public record JwtPrincipal(UUID userId, String username, java.util.List<String> roles) { }
