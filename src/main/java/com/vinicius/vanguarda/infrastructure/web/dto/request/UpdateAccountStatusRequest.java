package com.vinicius.vanguarda.infrastructure.web.dto.request;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "status is required")
        AccountStatus status
) {}
