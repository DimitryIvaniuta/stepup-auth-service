package com.github.dimitryivaniuta.gateway.stepupauth.service.outbox;

import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Publishes outbox events to Kafka with retry/backoff. */
@Slf4j
@Component
public class OutboxPublisher {
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    @Value("${app.outbox.enabled:true}") private boolean enabled;
    @Value("${app.outbox.batch-size:50}") private int batchSize;
    @Value("${app.kafka.topics.monitoring}") private String topic;

    public OutboxPublisher(OutboxEventRepository repo, KafkaTemplate<String,String> kafka) {
        this.repo = repo; this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval:2s}")
    @Transactional
    public void publishDueEvents() {
        if (!enabled) return;
        var batch = repo.findDueBatch(Instant.now(), List.of("NEW","FAILED"), batchSize);
        for (var e : batch) {
            try {
                kafka.send(topic, e.getAggregateId().toString(), e.getPayloadJson()).get();
                e.setStatus("PUBLISHED");
                e.setPublishedAt(Instant.now());
                repo.save(e);
            } catch (Exception ex) {
                int attempts = e.getAttempts() + 1;
                e.setAttempts(attempts);
                e.setStatus("FAILED");
                long backoffSeconds = Math.min(300, (long) Math.pow(2, Math.min(8, attempts)));
                e.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
                repo.save(e);
                log.warn("Outbox publish failed id={}, attempts={}, retryIn={}s", e.getId(), attempts, backoffSeconds, ex);
            }
        }
    }
}
