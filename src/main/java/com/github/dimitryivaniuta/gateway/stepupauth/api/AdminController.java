package com.github.dimitryivaniuta.gateway.stepupauth.api;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.MonitoringEventDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.OutboxEventDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.RiskDecisionDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.StepUpChallengeDto;
import com.github.dimitryivaniuta.gateway.stepupauth.service.admin.AdminOutboxCommandService;
import com.github.dimitryivaniuta.gateway.stepupauth.service.admin.AdminQueryService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin / operations endpoints.
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li>Controller is thin (HTTP only)</li>
 *   <li>Business logic lives in services</li>
 *   <li>Repositories are not referenced directly from controllers</li>
 * </ul>
 *
 * <p>Security: requires {@code ROLE_ADMIN} from JWT claim {@code roles}.</p>
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminQueryService queries;
    private final AdminOutboxCommandService outboxCommands;

    public AdminController(AdminQueryService queries, AdminOutboxCommandService outboxCommands) {
        this.queries = queries;
        this.outboxCommands = outboxCommands;
    }

    @GetMapping("/risk-decisions")
    public List<RiskDecisionDto> riskDecisions(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return queries.listRiskDecisions(limit);
    }

    @GetMapping("/stepup-challenges")
    public List<StepUpChallengeDto> stepupChallenges(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return queries.listStepUpChallenges(limit);
    }

    @GetMapping("/outbox")
    public List<OutboxEventDto> outbox(
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return queries.listOutbox(eventType, limit);
    }

    /**
     * Manually re-enqueue an outbox event. Useful for ops UI.
     *
     * <p>Semantics: sets status to NEW, clears lastError, and schedules immediate retry.</p>
     */
    @PostMapping("/outbox/{id}/retry")
    @Transactional
    public void retryOutbox(@PathVariable("id") long id) {
        outboxCommands.retry(id);
    }

    /**
     * Returns monitoring events (projection of monitoring outbox events).
     */
    @GetMapping("/monitoring/events")
    public List<MonitoringEventDto> monitoringEvents(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return queries.listMonitoringEvents(limit);
    }
}
