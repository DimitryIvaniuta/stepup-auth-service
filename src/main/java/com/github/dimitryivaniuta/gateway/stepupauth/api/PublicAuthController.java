package com.github.dimitryivaniuta.gateway.stepupauth.api;

import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.LoginRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.LoginResponse;
import com.github.dimitryivaniuta.gateway.stepupauth.api.dto.RegisterRequest;
import com.github.dimitryivaniuta.gateway.stepupauth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public endpoints: register + login. */
@RestController
@RequestMapping("/api/public")
public class PublicAuthController {
    private final AuthService auth;
    public PublicAuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        auth.register(req.username(), req.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password());
    }
}
