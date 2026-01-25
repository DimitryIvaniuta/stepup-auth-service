package com.github.dimitryivaniuta.gateway.stepupauth.service.decision;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.StepUpVerifyResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.RiskDecisionEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.StepUpChallengeEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.RiskDecisionRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.StepUpChallengeRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.service.monitoring.MonitoringEventFactory;
import com.github.dimitryivaniuta.gateway.stepupauth.service.monitoring.MonitoringEvents;
import com.github.dimitryivaniuta.gateway.stepupauth.service.otp.OtpService;
import com.github.dimitryivaniuta.gateway.stepupauth.service.outbox.OutboxService;
import com.github.dimitryivaniuta.gateway.stepupauth.service.trust.TrustService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Verifies OTP and finalizes the decision approval. */
@Service
public class StepUpService {
    private final StepUpChallengeRepository challenges;
    private final RiskDecisionRepository decisions;
    private final OtpService otp;
    private final TrustService trust;
    private final OutboxService outbox;
    private final MonitoringEventFactory events;

    public StepUpService(StepUpChallengeRepository challenges, RiskDecisionRepository decisions, OtpService otp,
                         TrustService trust, OutboxService outbox, MonitoringEventFactory events) {
        this.challenges = challenges; this.decisions = decisions; this.otp = otp; this.trust = trust;
        this.outbox = outbox; this.events = events;
    }

    @Transactional
    public StepUpVerifyResponse verify(UUID userId, UUID challengeId, String code) {
        StepUpChallengeEntity ch = challenges.findByIdAndUserId(challengeId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Challenge not found"));
        if (!"PENDING".equals(ch.getStatus())) throw new ApiException(HttpStatus.CONFLICT, "Challenge is not pending");

        otp.verify(challengeId, code);

        ch.setStatus("VERIFIED");
        ch.setVerifiedAt(Instant.now());
        challenges.save(ch);

        RiskDecisionEntity d = decisions.findById(ch.getDecisionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Decision not found"));
        d.setDecision("APPROVED");
        decisions.save(d);

        trust.trust(userId, d.getDeviceHash(), d.getCountry());

        var e = events.base(MonitoringEvents.STEP_UP_VERIFIED, userId, d.getId());
        e.putAll(Map.of("challengeId", challengeId.toString(), "decision", "APPROVED"));
        outbox.enqueue(d.getId(), MonitoringEvents.STEP_UP_VERIFIED, events.toJson(e));

        return new StepUpVerifyResponse("VERIFIED", d.getId());
    }
}
