package com.github.dimitryivaniuta.gateway.stepupauth.api;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.StepUpVerifyRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.StepUpVerifyResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.security.CurrentUser;
import com.github.dimitryivaniuta.gateway.stepupauth.service.decision.StepUpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** OTP verification endpoint for step-up. */
@RestController
@RequestMapping("/api/stepup")
public class StepUpController {
    private final StepUpService stepUp;
    public StepUpController(StepUpService stepUp) { this.stepUp = stepUp; }

    @PostMapping("/{challengeId}/verify")
    public StepUpVerifyResponse verify(@PathVariable UUID challengeId, @Valid @RequestBody StepUpVerifyRequest req) {
        return stepUp.verify(CurrentUser.userId().orElseThrow(), challengeId, req.otp());
    }
}
