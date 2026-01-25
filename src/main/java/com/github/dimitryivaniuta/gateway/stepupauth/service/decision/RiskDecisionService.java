package com.github.dimitryivaniuta.gateway.stepupauth.service.decision;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.RiskDecisionEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.domain.StepUpChallengeEntity;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.RiskDecisionRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.StepUpChallengeRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.service.monitoring.MonitoringEventFactory;
import com.github.dimitryivaniuta.gateway.stepupauth.service.monitoring.MonitoringEvents;
import com.github.dimitryivaniuta.gateway.stepupauth.service.otp.OtpService;
import com.github.dimitryivaniuta.gateway.stepupauth.service.outbox.OutboxService;
import com.github.dimitryivaniuta.gateway.stepupauth.service.risk.RiskEngine;
import com.github.dimitryivaniuta.gateway.stepupauth.service.signal.SignalHasher;
import com.github.dimitryivaniuta.gateway.stepupauth.service.trust.TrustService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Creates risk decisions and triggers step-up when risky. */
@Service
public class RiskDecisionService {
    private final RiskEngine riskEngine;
    private final TrustService trust;
    private final RiskDecisionRepository decisions;
    private final StepUpChallengeRepository challenges;
    private final OtpService otp;
    private final OutboxService outbox;
    private final MonitoringEventFactory events;

    @Value("${app.otp.dev-preview:false}") private boolean otpDevPreview;

    public RiskDecisionService(RiskEngine riskEngine, TrustService trust,
                               RiskDecisionRepository decisions, StepUpChallengeRepository challenges,
                               OtpService otp, OutboxService outbox, MonitoringEventFactory events) {
        this.riskEngine = riskEngine; this.trust = trust; this.decisions = decisions; this.challenges = challenges;
        this.otp = otp; this.outbox = outbox; this.events = events;
    }

    /** Authorize action: APPROVED or STEP_UP_REQUIRED. */
    @Transactional
    public AuthorizeTransactionResponse authorize(UUID userId, String deviceId, String country, AuthorizeTransactionRequest req) {
        if (deviceId == null || deviceId.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Missing X-Device-Id");
        if (country == null || country.isBlank() || country.length() != 2) throw new ApiException(HttpStatus.BAD_REQUEST, "Missing/invalid X-Country");

        String deviceHash = SignalHasher.sha256(deviceId.trim());
        String c = country.toUpperCase();

        boolean isNewDevice = !trust.isDeviceTrusted(userId, deviceHash);
        boolean isNewCountry = trust.isNewCountry(userId, c);

        var assessment = riskEngine.assess(isNewDevice, isNewCountry, req.amount());

        UUID decisionId = UUID.randomUUID();
        RiskDecisionEntity d = new RiskDecisionEntity();
        d.setId(decisionId);
        d.setUserId(userId);
        d.setActionType(req.actionType());
        d.setAmount(req.amount());
        d.setDeviceHash(deviceHash);
        d.setCountry(c);
        d.setRiskScore(assessment.score());
        d.setRiskLevel(assessment.level().name());
        d.setCreatedAt(Instant.now());

        if (assessment.stepUpRequired()) {
            UUID challengeId = UUID.randomUUID();

            StepUpChallengeEntity ch = new StepUpChallengeEntity();
            ch.setId(challengeId);
            ch.setUserId(userId);
            ch.setDecisionId(decisionId);
            ch.setStatus("PENDING");
            ch.setAttempts(0);
            ch.setCreatedAt(Instant.now());
            challenges.save(ch);

            d.setDecision("STEP_UP_REQUIRED");
            d.setStepUpRequired(true);
            d.setStepUpChallengeId(challengeId);
            decisions.save(d);

            String code = otp.generateAndStore(challengeId);

            var e1 = events.base(MonitoringEvents.RISK_DECISION_MADE, userId, decisionId);
            e1.putAll(Map.of("riskScore", assessment.score(), "riskLevel", assessment.level().name(), "reasons", assessment.reasons(), "decision", "STEP_UP_REQUIRED"));
            outbox.enqueue(decisionId, MonitoringEvents.RISK_DECISION_MADE, events.toJson(e1));

            var e2 = events.base(MonitoringEvents.STEP_UP_REQUIRED, userId, decisionId);
            e2.putAll(Map.of("challengeId", challengeId.toString(), "riskScore", assessment.score(), "reasons", assessment.reasons()));
            outbox.enqueue(decisionId, MonitoringEvents.STEP_UP_REQUIRED, events.toJson(e2));

            return new AuthorizeTransactionResponse("STEP_UP_REQUIRED", decisionId, assessment.score(), assessment.level().name(), challengeId, otpDevPreview ? code : null);
        }

        d.setDecision("APPROVED");
        d.setStepUpRequired(false);
        decisions.save(d);
        trust.trust(userId, deviceHash, c);

        var e1 = events.base(MonitoringEvents.RISK_DECISION_MADE, userId, decisionId);
        e1.putAll(Map.of("riskScore", assessment.score(), "riskLevel", assessment.level().name(), "reasons", assessment.reasons(), "decision", "APPROVED"));
        outbox.enqueue(decisionId, MonitoringEvents.RISK_DECISION_MADE, events.toJson(e1));

        return new AuthorizeTransactionResponse("APPROVED", decisionId, assessment.score(), assessment.level().name(), null, null);
    }
}
