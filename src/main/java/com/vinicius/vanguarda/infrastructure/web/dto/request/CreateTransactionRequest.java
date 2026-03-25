package com.vinicius.vanguarda.infrastructure.web.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull(message = "sourceAccountId is required")
        UUID sourceAccountId,

        @NotNull(message = "targetAccountId is required")
        UUID targetAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be > 0")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 255, message = "description must not exceed 255 characters")
        String description,

        Map<String, Object> metadata
) {}
