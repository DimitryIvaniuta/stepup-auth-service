package com.github.dimitryivaniuta.gateway.stepupauth.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Accessor for current user id. */
public final class CurrentUser {
    private CurrentUser() { }
    public static Optional<UUID> userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) return Optional.empty();
        return Optional.of(p.userId());
    }
}
