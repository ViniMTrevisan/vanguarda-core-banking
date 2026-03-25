package com.vinicius.vanguarda.infrastructure.web.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId
) {}
