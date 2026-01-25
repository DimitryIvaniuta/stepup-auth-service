package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Step-up challenge audit record (OTP secret lives in Redis only). */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="step_up_challenge")
public class StepUpChallengeEntity {
    @Id private UUID id;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="decision_id", nullable=false) private UUID decisionId;
    @Column(nullable=false, length=20) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="verified_at") private Instant verifiedAt;
}
