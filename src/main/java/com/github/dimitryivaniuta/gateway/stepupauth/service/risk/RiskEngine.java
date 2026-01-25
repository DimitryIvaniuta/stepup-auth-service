package com.github.dimitryivaniuta.gateway.stepupauth.service.risk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Risk scoring engine based on configurable signals. */
@Component
public class RiskEngine {
    @Value("${app.risk.high-amount-threshold}") private BigDecimal highAmountThreshold;
    @Value("${app.risk.step-up-score-threshold}") private int stepUpThreshold;
    @Value("${app.risk.signals.new-device-score}") private int newDeviceScore;
    @Value("${app.risk.signals.new-country-score}") private int newCountryScore;
    @Value("${app.risk.signals.high-amount-score}") private int highAmountScore;

    public RiskAssessment assess(boolean isNewDevice, boolean isNewCountry, BigDecimal amount) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (isNewDevice) { score += newDeviceScore; reasons.add("NEW_DEVICE"); }
        if (isNewCountry) { score += newCountryScore; reasons.add("NEW_COUNTRY"); }
        if (amount != null && amount.compareTo(highAmountThreshold) >= 0) { score += highAmountScore; reasons.add("HIGH_AMOUNT"); }
        RiskLevel level = score >= stepUpThreshold ? RiskLevel.HIGH : (score >= (stepUpThreshold/2) ? RiskLevel.MEDIUM : RiskLevel.LOW);
        return new RiskAssessment(score, level, score >= stepUpThreshold, String.join(",", reasons));
    }
}
