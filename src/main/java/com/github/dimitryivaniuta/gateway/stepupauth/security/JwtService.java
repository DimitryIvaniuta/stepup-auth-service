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
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/** Issues and validates JWT tokens (HS256). */
@Service
public class JwtService {
    @Value("${app.security.jwt.issuer}") private String issuer;
    @Value("${app.security.jwt.secret}") private String secret;
    @Value("${app.security.jwt.ttl}") private Duration ttl;
    private SecretKey key;

    @PostConstruct void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Issues JWT for user. */
    public String issueToken(UUID userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setIssuer(issuer)
            .setSubject(userId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(ttl)))
            .addClaims(Map.of("username", username))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /** Parses JWT and returns principal. */
    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)          // key is SecretKey
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtPrincipal(
                UUID.fromString(claims.getSubject()),
                (String) claims.get("username")
        );
    }
}
