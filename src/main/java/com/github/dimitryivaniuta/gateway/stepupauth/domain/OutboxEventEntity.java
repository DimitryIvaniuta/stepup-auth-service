package com.github.dimitryivaniuta.gateway.stepupauth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Transactional outbox row for Kafka publishing. */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="outbox_event")
public class OutboxEventEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="aggregate_id", nullable=false) private UUID aggregateId;
    @Column(name="event_type", nullable=false, length=100) private String eventType;
    @Column(name="payload_json", nullable=false, columnDefinition="jsonb") private String payloadJson;
    @Column(nullable=false, length=20) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at", nullable=false) private Instant nextAttemptAt;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="published_at") private Instant publishedAt;
}
