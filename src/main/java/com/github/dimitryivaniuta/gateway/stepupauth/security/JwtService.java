package com.github.dimitryivaniuta.gateway.stepupauth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWT tokens (HS256).
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>JWT contains {@code sub=userId}</li>
 *   <li>JWT contains {@code username}</li>
 *   <li>JWT contains {@code roles} (list of strings), always at least {@code USER}</li>
 * </ul>
 */
@Service
public class JwtService {

    @Value("${app.security.jwt.issuer}")
    private String issuer;

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.ttl}")
    private Duration ttl;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a JWT for a user.
     *
     * <p>Important: if roles are null/empty, we still issue token with {@code ["USER"]},
     * so that endpoints protected with {@code hasRole('USER')} do not return 403.</p>
     */
    public String issueToken(UUID userId, String username, List<String> roles) {
        List<String> normalizedRoles = normalizeRoles(roles);

        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .addClaims(Map.of(
                        "username", username,
                        "roles", normalizedRoles
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses JWT and returns principal.
     */
    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = (String) claims.get("username");
        UUID userId = UUID.fromString(claims.getSubject());

        List<String> roles = extractRoles(claims);
        roles = normalizeRoles(roles);

        return new JwtPrincipal(userId, username, roles);
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of("USER");
        }
        List<String> cleaned = roles.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .map(r -> r.startsWith("ROLE_") ? r.substring("ROLE_".length()) : r) // store without ROLE_ in token
                .distinct()
                .toList();

        return cleaned.isEmpty() ? List.of("USER") : cleaned;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(Claims claims) {
        Object rawRoles = claims.get("roles");

        if (rawRoles instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        if (rawRoles instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isBlank())
                    .toList();
        }
        return List.of("USER");
    }
}
