package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Trusted device hash for user. */
@Getter @Setter @NoArgsConstructor
@Entity
@Table(name="trusted_device", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","device_hash"}))
public class TrustedDeviceEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="device_hash", nullable=false, length=128) private String deviceHash;
    @Column(name="first_seen_at", nullable=false) private Instant firstSeenAt;
    @Column(name="last_seen_at", nullable=false) private Instant lastSeenAt;
}
