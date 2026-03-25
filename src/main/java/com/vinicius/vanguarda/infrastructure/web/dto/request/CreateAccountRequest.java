package com.vinicius.vanguarda.infrastructure.web.dto.request;

import com.vinicius.vanguarda.domain.model.enums.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "ownerId is required")
        String ownerId,

        @NotBlank(message = "ownerName is required")
        @Size(min = 3, max = 100, message = "ownerName must be between 3 and 100 characters")
        String ownerName,

        @NotNull(message = "currency is required")
        Currency currency,

        @NotNull(message = "initialBalance is required")
        @DecimalMin(value = "0.00", message = "initialBalance must be >= 0")
        @Digits(integer = 17, fraction = 2, message = "initialBalance must have at most 2 decimal places")
        BigDecimal initialBalance
) {}
