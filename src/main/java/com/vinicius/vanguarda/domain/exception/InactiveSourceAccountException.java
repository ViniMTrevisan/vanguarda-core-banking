package com.vinicius.vanguarda.domain.exception;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;

import java.util.UUID;

public class InactiveSourceAccountException extends RuntimeException {
    public InactiveSourceAccountException(UUID accountId, AccountStatus status) {
        super("Source account " + accountId + " is not ACTIVE (status: " + status + ")");
    }
}
