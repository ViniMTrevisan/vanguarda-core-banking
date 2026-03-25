package com.vinicius.vanguarda.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "clientId is required")
        String clientId,

        @NotBlank(message = "clientSecret is required")
        String clientSecret
) {}
