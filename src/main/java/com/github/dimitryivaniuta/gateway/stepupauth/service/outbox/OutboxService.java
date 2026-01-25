package com.github.dimitryivaniuta.gateway.stepupauth.service.outbox;

import com.github.dimitryivaniuta.gateway.stepupauth.domain.OutboxEventEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Writes outbox events in DB transaction. */
@Service
public class OutboxService {
    private final OutboxEventRepository repo;
    public OutboxService(OutboxEventRepository repo) { this.repo = repo; }

    @Transactional
    public void enqueue(UUID aggregateId, String eventType, String payloadJson) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        e.setPayloadJson(payloadJson);
        e.setStatus("NEW");
        e.setAttempts(0);
        e.setNextAttemptAt(Instant.now());
        e.setCreatedAt(Instant.now());
        repo.save(e);
    }
}
