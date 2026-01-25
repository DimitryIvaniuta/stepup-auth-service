package com.github.dimitryivaniuta.gateway.stepupauth.service.risk;

/** Risk assessment result. */
public record RiskAssessment(int score, RiskLevel level, boolean stepUpRequired, String reasons) { }
