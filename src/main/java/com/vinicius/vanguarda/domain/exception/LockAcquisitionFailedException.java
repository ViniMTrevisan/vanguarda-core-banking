package com.vinicius.vanguarda.domain.exception;

public class LockAcquisitionFailedException extends RuntimeException {
    public LockAcquisitionFailedException(String accountId) {
        super("Failed to acquire distributed lock for account: " + accountId);
    }
}
