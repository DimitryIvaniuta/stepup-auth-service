package com.github.dimitryivaniuta.gateway.stepupauth.service;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.LoginResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.UserEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.UserRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Primary auth (username/password -> JWT). */
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional
    public void register(String username, String rawPassword) {
        if (users.findByUsername(username).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        // Demo bootstrap: treat username 'admin' as an admin user.
        // In production this would come from RBAC or an external IdP.
        u.setRoles("admin".equalsIgnoreCase(username) ? "USER,ADMIN" : "USER");
        u.setCreatedAt(Instant.now());
        users.save(u);
    }

    public LoginResponse login(String username, String rawPassword) {
        UserEntity u = users.findByUsername(username)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(rawPassword, u.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        java.util.List<String> roles = java.util.Arrays.stream((u.getRoles() == null ? "USER" : u.getRoles()).split(","))
            .map(String::trim)
            .filter(r -> !r.isBlank())
            .toList();
        return new LoginResponse(u.getId(), jwt.issueToken(u.getId(), u.getUsername(), roles));
    }
}
