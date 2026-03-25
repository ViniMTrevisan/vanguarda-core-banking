package com.vinicius.vanguarda.domain.exception;

public class TransactionInProgressException extends RuntimeException {
    public TransactionInProgressException(String idempotencyKey) {
        super("Transaction with idempotency key '" + idempotencyKey + "' is currently being processed");
    }
}
