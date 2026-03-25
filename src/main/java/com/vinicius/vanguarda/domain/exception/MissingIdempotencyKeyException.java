package com.vinicius.vanguarda.domain.exception;

public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException() {
        super("X-Idempotency-Key header is required and must be a valid UUID v4");
    }
}
