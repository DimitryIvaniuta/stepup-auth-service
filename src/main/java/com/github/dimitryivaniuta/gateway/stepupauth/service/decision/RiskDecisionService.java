package com.github.dimitryivaniuta.gateway.stepupauth.service.decision;

import com.github.dimitryivaniuta.gateway.stepupauth.api.ApiException;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.config.props.OtpProperties;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Creates risk decisions and triggers step-up authentication when actions are risky.
 *
 * <p>Acceptance:
 * <ul>
 *   <li>Risky actions => STEP_UP_REQUIRED (OTP/2FA)</li>
 *   <li>Safe actions => APPROVED (smooth UX)</li>
 *   <li>Decision is stored (audit) + event is published (monitoring)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDecisionService {

    private static final String HDR_DEVICE = "X-Device-Id";
    private static final String HDR_COUNTRY = "X-Country";

    private final RiskEngine riskEngine;
    private final TrustService trust;
    private final RiskDecisionRepository decisions;
    private final StepUpChallengeRepository challenges;
    private final OtpService otp;
    private final OutboxService outbox;
    private final MonitoringEventFactory events;
    private final OtpProperties otpProps;

    /**
     * Authorize an action based on risk signals.
     *
     * @param userId authenticated user id
     * @param deviceId device id header (raw), will be hashed
     * @param country ISO-2 country header
     * @param req action payload
     * @return decision response
     */
    @Transactional
    public AuthorizeTransactionResponse authorize(UUID userId,
                                                  String deviceId,
                                                  String country,
                                                  AuthorizeTransactionRequest req) {

        requireHeader(deviceId, HDR_DEVICE, "Missing " + HDR_DEVICE);
        requireHeader(country, HDR_COUNTRY, "Missing/invalid " + HDR_COUNTRY + " (ISO-2)");

        final String deviceHash = SignalHasher.sha256(deviceId.trim());
        final String c = country.trim().toUpperCase();

        final boolean isNewDevice = !trust.isDeviceTrusted(userId, deviceHash);
        final boolean isNewCountry = trust.isNewCountry(userId, c);

        final var assessment = riskEngine.assess(isNewDevice, isNewCountry, req.amount());

        final UUID decisionId = UUID.randomUUID();
        final RiskDecisionEntity d = new RiskDecisionEntity();
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
            // FIX: save decision first (must exist before challenge FK insert)
            d.setDecision("STEP_UP_REQUIRED");
            d.setStepUpRequired(true);
            decisions.saveAndFlush(d);

            final UUID challengeId = UUID.randomUUID();

            final StepUpChallengeEntity ch = new StepUpChallengeEntity();
            ch.setId(challengeId);
            ch.setUserId(userId);
            ch.setDecisionId(decisionId);
            ch.setStatus("PENDING");
            ch.setAttempts(0);
            ch.setCreatedAt(Instant.now());
            challenges.save(ch);

            // back-reference stored decision -> challenge id
            d.setStepUpChallengeId(challengeId);
            decisions.save(d);

            final String code = otp.generateAndStore(challengeId);

            enqueueDecisionMade(userId, decisionId, assessment.score(), assessment.level().name(), assessment.reasons(), "STEP_UP_REQUIRED");
            enqueueStepUpRequired(userId, decisionId, challengeId, assessment.score(), assessment.reasons());

            log.info("Step-up required: userId={}, decisionId={}, challengeId={}, score={}, reasons={}",
                    userId, decisionId, challengeId, assessment.score(), assessment.reasons());

            return new AuthorizeTransactionResponse(
                    "STEP_UP_REQUIRED",
                    decisionId,
                    assessment.score(),
                    assessment.level().name(),
                    challengeId,
                    otpProps.devPreview() ? code : null
            );
        }

        // Safe path: approve and trust signals immediately (UX-friendly)
        d.setDecision("APPROVED");
        d.setStepUpRequired(false);
        decisions.save(d);

        trust.trust(userId, deviceHash, c);

        enqueueDecisionMade(userId, decisionId, assessment.score(), assessment.level().name(), assessment.reasons(), "APPROVED");

        log.info("Approved: userId={}, decisionId={}, score={}, reasons={}",
                userId, decisionId, assessment.score(), assessment.reasons());

        return new AuthorizeTransactionResponse(
                "APPROVED",
                decisionId,
                assessment.score(),
                assessment.level().name(),
                null,
                null
        );
    }

    private static void requireHeader(String value, String headerName, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        if (headerName.equals(HDR_COUNTRY) && value.trim().length() != 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void enqueueDecisionMade(UUID userId,
                                     UUID decisionId,
                                     int score,
                                     String level,
                                     String reasons,
                                     String decision) {
        var payload = events.base(MonitoringEvents.RISK_DECISION_MADE, userId, decisionId);
        payload.putAll(Map.of(
                "riskScore", score,
                "riskLevel", level,
                "reasons", reasons,
                "decision", decision
        ));
        outbox.enqueue(decisionId, MonitoringEvents.RISK_DECISION_MADE, events.toJson(payload));
    }

    private void enqueueStepUpRequired(UUID userId,
                                       UUID decisionId,
                                       UUID challengeId,
                                       int score,
                                       String reasons) {
        var payload = events.base(MonitoringEvents.STEP_UP_REQUIRED, userId, decisionId);
        payload.putAll(Map.of(
                "challengeId", challengeId.toString(),
                "riskScore", score,
                "reasons", reasons
        ));
        outbox.enqueue(decisionId, MonitoringEvents.STEP_UP_REQUIRED, events.toJson(payload));
    }
}
