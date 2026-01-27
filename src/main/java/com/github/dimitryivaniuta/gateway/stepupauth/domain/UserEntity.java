package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Application user.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    /**
     * Comma-separated roles (e.g. {@code USER,ADMIN}).
     *
     * <p>Stored as a simple string to keep the demo lightweight; in larger systems this would
     * typically be normalized (user_roles table) or sourced from an external IdP.</p>
     */
    @Column(nullable = false, length = 200)
    private String roles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
