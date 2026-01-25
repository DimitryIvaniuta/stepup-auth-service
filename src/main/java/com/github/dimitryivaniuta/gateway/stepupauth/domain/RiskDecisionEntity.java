package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Stored risk decision (audit). */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="risk_decision")
public class RiskDecisionEntity {
    @Id private UUID id;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="action_type", nullable=false, length=50) private String actionType;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(name="device_hash", nullable=false, length=128) private String deviceHash;
    @Column(nullable=false, length=2) private String country;
    @Column(name="risk_score", nullable=false) private int riskScore;
    @Column(name="risk_level", nullable=false, length=20) private String riskLevel;
    @Column(nullable=false, length=20) private String decision;
    @Column(name="step_up_required", nullable=false) private boolean stepUpRequired;
    @Column(name="step_up_challenge_id") private UUID stepUpChallengeId;
    @Column(name="created_at", nullable=false) private Instant createdAt;
}
