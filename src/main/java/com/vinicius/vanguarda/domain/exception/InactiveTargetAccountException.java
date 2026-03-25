package com.vinicius.vanguarda.domain.exception;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;

import java.util.UUID;

public class InactiveTargetAccountException extends RuntimeException {
    public InactiveTargetAccountException(UUID accountId, AccountStatus status) {
        super("Target account " + accountId + " is CLOSED and cannot receive funds (status: " + status + ")");
    }
}
