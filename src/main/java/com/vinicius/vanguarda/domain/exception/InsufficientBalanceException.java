package com.vinicius.vanguarda.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID accountId, BigDecimal balance, BigDecimal required) {
        super("Account " + accountId + " has insufficient balance: " + balance + " < " + required);
    }
}
