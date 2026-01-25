package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Application user. */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="app_user")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=100) private String username;
    @Column(name="password_hash", nullable=false, length=200) private String passwordHash;
    @Column(name="created_at", nullable=false) private Instant createdAt;
}
