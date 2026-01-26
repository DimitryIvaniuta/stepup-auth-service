package com.github.dimitryivaniuta.gateway.stepupauth.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.AppKafkaProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.OutboxProperties;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Publishes outbox events to Kafka with retry/backoff.
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final List<String> DUE_STATUSES = List.of("NEW", "FAILED");

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final OutboxProperties outbox;
    private final AppKafkaProperties appKafka;

    private final PlatformTransactionManager txManager;

    /**
     * Periodically publishes due outbox events to Kafka.
     *
     * <p>Design note: we keep DB operations transactional, but Kafka I/O is done outside the DB transaction to
     * avoid holding DB locks/connection during network calls.</p>
     */
    @Scheduled(fixedDelayString = "${app.outbox.publish-interval}")
    public void publishDueEvents() {
        if (!outbox.enabled()) {
            return;
        }

        // 1) Load batch (in DB tx)
        var tx = new TransactionTemplate(txManager);
        var batch = tx.execute(status ->
                repository.findDueBatch(Instant.now(), DUE_STATUSES, outbox.batchSize())
        );

        if (batch == null || batch.isEmpty()) {
            return;
        }

        // 2) Publish each event (no DB tx)
        for (var e : batch) {
            try {
                String payload = objectMapper.writeValueAsString(e.getPayloadJson());
                kafkaTemplate.send(appKafka.topics().monitoring(), e.getAggregateId().toString(), payload).get();

                // 3) Mark published (DB tx)
                tx.executeWithoutResult(status -> {
                    e.setStatus("PUBLISHED");
                    e.setPublishedAt(Instant.now());
                    repository.save(e);
                });

                log.info("Outbox published: id={}, type={}, aggregateId={}", e.getId(), e.getEventType(), e.getAggregateId());
            } catch (Exception ex) {
                tx.executeWithoutResult(status -> {
                    int attempts = e.getAttempts() + 1;
                    e.setAttempts(attempts);
                    e.setStatus("FAILED");
                    e.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(attempts)));
                    repository.save(e);
                });

                log.warn("Outbox publish failed: id={}, attempts={}, nextAttemptAt={}, cause={}",
                        e.getId(), e.getAttempts(), e.getNextAttemptAt(), ex.toString());
            }
        }
    }

    /**
     * Exponential backoff with cap (seconds).
     */
    static long backoffSeconds(int attempts) {
        long base = (long) Math.pow(2, Math.min(10, attempts)); // 2..1024
        return Math.min(300, Math.max(2, base)); // cap 5 min, min 2s
    }
}
