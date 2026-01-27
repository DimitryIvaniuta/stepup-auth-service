package com.github.dimitryivaniuta.gateway.stepupauth.service.admin;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.MonitoringEventDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.OutboxEventDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.RiskDecisionDto;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.StepUpChallengeDto;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.OutboxEventEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.OutboxEventRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.RiskDecisionRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.StepUpChallengeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only admin queries.
 *
 * <p>This service is the correct place to call query helpers like
 * {@code OutboxEventRepository.findRecent(...)} and {@code findRecentByEventType(...)}.</p>
 */
@Service
public class AdminQueryService {

    private final RiskDecisionRepository decisions;
    private final StepUpChallengeRepository challenges;
    private final OutboxEventRepository outbox;

    public AdminQueryService(
            RiskDecisionRepository decisions,
            StepUpChallengeRepository challenges,
            OutboxEventRepository outbox
    ) {
        this.decisions = decisions;
        this.challenges = challenges;
        this.outbox = outbox;
    }

    public List<RiskDecisionDto> listRiskDecisions(int limit) {
        int safe = clamp(limit, 1, 500);

        return decisions.findTop500ByOrderByCreatedAtDesc()
                .stream()
                .limit(safe)
                .map(e -> new RiskDecisionDto(
                        e.getId(),
                        e.getUserId(),
                        e.getActionType(),
                        e.getAmount(),
                        e.getCountry(),
                        e.getRiskScore(),
                        e.getRiskLevel(),
                        e.getDecision(),
                        e.getCreatedAt(),
                        e.getStepUpChallengeId()
                ))
                .toList();
    }

    public List<StepUpChallengeDto> listStepUpChallenges(int limit) {
        int safe = clamp(limit, 1, 500);

        return challenges.findTop500ByOrderByCreatedAtDesc()
                .stream()
                .limit(safe)
                .map(e -> new StepUpChallengeDto(
                        e.getId(),
                        e.getUserId(),
                        e.getDecisionId(),
                        e.getStatus(),
                        e.getAttempts(),
                        e.getCreatedAt(),
                        e.getVerifiedAt()
                ))
                .toList();
    }

    public List<OutboxEventDto> listOutbox(String eventType, int limit) {
        int safe = clamp(limit, 1, 500);
        var page = PageRequest.of(0, safe);

        var rows = (eventType == null || eventType.isBlank())
                ? outbox.findRecent(page).getContent()
                : outbox.findRecentByEventType(eventType.trim(), page).getContent();

        return rows.stream().map(AdminQueryService::toDto).toList();
    }

    public List<MonitoringEventDto> listMonitoringEvents(int limit) {
        int safe = clamp(limit, 1, 500);

        // Read recent outbox entries and filter by "monitoring-like" event types.
        // (You can replace this with a dedicated repo query if needed.)
        var rows = outbox.findRecent(PageRequest.of(0, 500)).getContent();

        return rows.stream()
                .filter(e -> isMonitoringType(e.getEventType()))
                .limit(safe)
                .map(e -> new MonitoringEventDto(
                        e.getId(),
                        e.getEventType(),
                        e.getCreatedAt(),
                        e.getPayloadJson()
                ))
                .toList();
    }

    private static boolean isMonitoringType(String eventType) {
        return eventType != null && (eventType.startsWith("RISK_") || eventType.startsWith("STEP_"));
    }

    private static OutboxEventDto toDto(OutboxEventEntity e) {
        return new OutboxEventDto(
                e.getId(),
                e.getAggregateId(),
                e.getEventType(),
                e.getPayloadJson(),
                e.getStatus(),
                e.getAttempts(),
                e.getCreatedAt(),
                e.getNextAttemptAt(),
                e.getPublishedAt(),
                e.getLastError()
        );
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(Math.max(v, min), max);
    }
}
