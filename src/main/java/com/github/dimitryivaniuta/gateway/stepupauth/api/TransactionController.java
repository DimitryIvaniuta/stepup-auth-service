package com.github.dimitryivaniuta.gateway.stepupauth.api;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.AuthorizeTransactionResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.security.CurrentUser;
import com.github.dimitryivaniuta.gateway.stepupauth.service.decision.RiskDecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Risky action authorization endpoint.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final RiskDecisionService decisions;

    public TransactionController(RiskDecisionService decisions) {
        this.decisions = decisions;
    }

    @PostMapping("/authorize")
    public AuthorizeTransactionResponse authorize(@RequestHeader("X-Device-Id") String deviceId,
                                                  @RequestHeader("X-Country") String country,
                                                  @Valid @RequestBody AuthorizeTransactionRequest req) {
        return decisions.authorize(CurrentUser.userId().orElseThrow(), deviceId, country, req);
    }
}
