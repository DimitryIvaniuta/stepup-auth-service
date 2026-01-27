package com.github.dimitryivaniuta.gateway.stepupauth.service.outbox;

import com.github.dimitryivaniuta.gateway.stepupauth.config.props.AppKafkaProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.OutboxProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes outbox events to Kafka with retry/backoff.
 */
@Slf4j
@Component
public class OutboxPublisher {
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final OutboxProperties outboxProps;
    private final AppKafkaProperties kafkaProps;

    public OutboxPublisher(
            OutboxEventRepository repo, KafkaTemplate<String, String> kafka,
            OutboxProperties outboxProps, AppKafkaProperties kafkaProps
    ) {
        this.repo = repo;
        this.kafka = kafka;
        this.outboxProps = outboxProps;
        this.kafkaProps = kafkaProps;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval:5s}")
    @Transactional
    public void publishDueEvents() {
        if (!outboxProps.enabled()) {
            return;
        }
        var batch = repo.findDueBatch(Instant.now(), List.of("NEW", "FAILED"), outboxProps.batchSize());
        for (var e : batch) {
            try {
                kafka.send(kafkaProps.topics().monitoring(), e.getAggregateId().toString(), e.getPayloadJson()).get();
                e.setStatus("PUBLISHED");
                e.setPublishedAt(Instant.now());
                e.setLastError(null);
                repo.save(e);
            } catch (Exception ex) {
                int attempts = e.getAttempts() + 1;
                e.setAttempts(attempts);
                e.setStatus("FAILED");
                long backoffSeconds = Math.min(300, (long) Math.pow(2, Math.min(8, attempts)));
                e.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
                e.setLastError(ex.getMessage());
                repo.save(e);
                log.warn("Outbox publish failed id={}, attempts={}, retryIn={}s", e.getId(), attempts, backoffSeconds, ex);
            }
        }
    }
}
