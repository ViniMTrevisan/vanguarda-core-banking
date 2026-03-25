package com.vinicius.vanguarda.infrastructure.web.dto.response;

public record AuthResponse(
        String token,
        long expiresIn
) {}
