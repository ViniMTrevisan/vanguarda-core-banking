package com.vinicius.vanguarda.infrastructure.web.exception;

import com.vinicius.vanguarda.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleTransactionNotFound(TransactionNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE",
                "Source account does not have sufficient balance for this transaction", req);
    }

    @ExceptionHandler(InactiveSourceAccountException.class)
    public ResponseEntity<ApiError> handleInactiveSource(InactiveSourceAccountException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INACTIVE_SOURCE_ACCOUNT", ex.getMessage(), req);
    }

    @ExceptionHandler(InactiveTargetAccountException.class)
    public ResponseEntity<ApiError> handleInactiveTarget(InactiveTargetAccountException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INACTIVE_TARGET_ACCOUNT", ex.getMessage(), req);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiError> handleCurrencyMismatch(CurrencyMismatchException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_MISMATCH", ex.getMessage(), req);
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<ApiError> handleSameAccount(SameAccountTransferException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "SAME_ACCOUNT_TRANSFER", ex.getMessage(), req);
    }

    @ExceptionHandler(TransactionInProgressException.class)
    public ResponseEntity<ApiError> handleInProgress(TransactionInProgressException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "TRANSACTION_IN_PROGRESS", ex.getMessage(), req);
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<ApiError> handleMissingKey(MissingIdempotencyKeyException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", ex.getMessage(), req);
    }

    @ExceptionHandler(LockAcquisitionFailedException.class)
    public ResponseEntity<ApiError> handleLockFailed(LockAcquisitionFailedException ex, HttpServletRequest req) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "LOCK_ACQUISITION_FAILED", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                req.getRequestURI(),
                UUID.randomUUID().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}
