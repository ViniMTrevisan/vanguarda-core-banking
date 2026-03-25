package com.vinicius.vanguarda.domain.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Source and target accounts must be different");
    }
}
