package com.github.dimitryivaniuta.gateway.stepupauth.service.admin;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Admin outbox commands (write operations).
 */
@Service
public class AdminOutboxCommandService {

    private final OutboxEventRepository outbox;

    public AdminOutboxCommandService(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    /**
     * Retry an outbox event by setting it to NEW and scheduling immediate publish.
     */
    public void retry(long id) {
        var e = outbox.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Outbox event not found"));

        e.setStatus("NEW");
        e.setNextAttemptAt(Instant.now());
        e.setLastError(null);

        outbox.save(e);
    }
}
