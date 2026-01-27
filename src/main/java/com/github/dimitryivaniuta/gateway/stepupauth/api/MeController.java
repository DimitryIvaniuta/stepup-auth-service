package com.github.dimitryivaniuta.gateway.stepupauth.api;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.RiskDecisionDto;
import com.github.dimitryivaniuta.gateway.stepupauth.repo.RiskDecisionRepository;
import com.github.dimitryivaniuta.gateway.stepupauth.security.JwtPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Authenticated user endpoints. */
@RestController
@RequestMapping("/api/me")
public class MeController {
    private final RiskDecisionRepository decisions;

    public MeController(RiskDecisionRepository decisions) { this.decisions = decisions; }

    /** Returns last N risk decisions for the current user. */
    @GetMapping("/risk-decisions")
    public List<RiskDecisionDto> myDecisions(@AuthenticationPrincipal JwtPrincipal principal) {
        return decisions.findTop200ByUserIdOrderByCreatedAtDesc(principal.userId())
            .stream()
            .map(e -> new RiskDecisionDto(
                e.getId(), e.getUserId(), e.getActionType(), e.getAmount(), e.getCountry(),
                e.getRiskScore(), e.getRiskLevel(), e.getDecision(), e.getCreatedAt(), e.getStepUpChallengeId()
            ))
            .toList();
    }
}
