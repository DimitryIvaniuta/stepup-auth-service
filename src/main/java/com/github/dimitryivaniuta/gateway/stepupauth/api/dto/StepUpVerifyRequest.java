package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import jakarta.validation.constraints.NotBlank;

/** OTP verify request. */
public record StepUpVerifyRequest(@NotBlank String otp) { }
