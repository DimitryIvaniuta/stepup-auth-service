package com.github.dimitryivaniuta.gateway.stepupauth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Register request. */
public record RegisterRequest(@NotBlank @Size(min=3,max=100) String username,
                              @NotBlank @Size(min=8,max=100) String password) { }
