package com.vinicius.vanguarda.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinicius.vanguarda.domain.exception.*;
import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.Money;
import com.vinicius.vanguarda.domain.model.Transaction;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import com.vinicius.vanguarda.domain.repository.LedgerEntryRepository;
import com.vinicius.vanguarda.domain.repository.TransactionRepository;
import com.vinicius.vanguarda.domain.service.DistributedLockProvider;
import com.vinicius.vanguarda.domain.service.EventPublisher;
import com.vinicius.vanguarda.domain.service.IdempotencyProvider;
import com.vinicius.vanguarda.infrastructure.messaging.event.TransactionCompletedEvent;
import com.vinicius.vanguarda.infrastructure.metrics.TransactionMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferMoneyUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferMoneyUseCase.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyProvider idempotencyProvider;
    private final DistributedLockProvider distributedLockProvider;
    private final EventPublisher eventPublisher;
    private final TransactionMetrics metrics;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final int transferRetryAttempts;
    private final long transferRetryBackoffMs;

    public TransferMoneyUseCase(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 LedgerEntryRepository ledgerEntryRepository,
                                 IdempotencyProvider idempotencyProvider,
                                 DistributedLockProvider distributedLockProvider,
                                 EventPublisher eventPublisher,
                                 TransactionMetrics metrics,
                                 ObjectMapper objectMapper,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${vcb.transfer.retry-attempts:5}") int transferRetryAttempts,
                                 @Value("${vcb.transfer.retry-backoff-ms:50}") long transferRetryBackoffMs) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyProvider = idempotencyProvider;
        this.distributedLockProvider = distributedLockProvider;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.transferRetryAttempts = Math.max(1, transferRetryAttempts);
        this.transferRetryBackoffMs = Math.max(0, transferRetryBackoffMs);
    }

    public Output execute(Input input) {
        // Step 1: Validate idempotency key format
        validateIdempotencyKey(input.idempotencyKey());

        // Step 2: Check idempotency cache
        Optional<String> cachedResponse = idempotencyProvider.getCompletedResponse(input.idempotencyKey());
        if (cachedResponse.isPresent()) {
            metrics.recordIdempotencyHit(true);
            return deserializeOutput(cachedResponse.get()).asReplay();
        }
        if (idempotencyProvider.isProcessing(input.idempotencyKey())) {
            metrics.recordIdempotencyHit(false);
            throw new TransactionInProgressException(input.idempotencyKey());
        }
        idempotencyProvider.markProcessing(input.idempotencyKey());

        Timer.Sample sample = metrics.startTransactionTimer();
        try {
            Output output = executeTransferWithRetry(input);

            // Step 6: Update idempotency cache
            cacheResponse(input.idempotencyKey(), output);

            // Step 7: Publish event (non-blocking, failures logged)
            publishEventSafely(output);

            // Step 8: Return 201
            metrics.recordTransaction("COMPLETED");
            return output;
        } catch (RuntimeException e) {
            metrics.recordTransaction("FAILED");
            throw e;
        } finally {
            metrics.stopTransactionTimer(sample);
        }
    }

    private Output executeTransferWithRetry(Input input) {
        RuntimeException lastTransientFailure = null;

        for (int attempt = 1; attempt <= transferRetryAttempts; attempt++) {
            try {
                return executeSingleAttempt(input);
            } catch (RuntimeException e) {
                if (!isTransientConcurrencyFailure(e)) {
                    throw e;
                }
                lastTransientFailure = e;
                if (attempt == transferRetryAttempts) {
                    throw e;
                }
                log.warn("Transient serialization conflict (attempt {}/{}), retrying transfer for idempotencyKey={}",
                        attempt, transferRetryAttempts, input.idempotencyKey());
                sleepBeforeRetry();
            }
        }

        throw lastTransientFailure != null
                ? lastTransientFailure
                : new IllegalStateException("Transfer retry loop exited unexpectedly");
    }

    private Output executeSingleAttempt(Input input) {
        // Determine lock order (lexicographic UUID to prevent deadlock)
        String firstLockKey = getLockKey(input.sourceAccountId(), input.targetAccountId(), true);
        String secondLockKey = getLockKey(input.sourceAccountId(), input.targetAccountId(), false);

        // Step 3: Acquire distributed locks
        boolean firstLockAcquired = acquireLockSafely(firstLockKey);
        boolean secondLockAcquired = false;

        try {
            if (!firstLockAcquired) {
                metrics.recordLockFailure();
                throw new LockAcquisitionFailedException(input.sourceAccountId().toString());
            }
            secondLockAcquired = acquireLockSafely(secondLockKey);
            if (!secondLockAcquired) {
                metrics.recordLockFailure();
                throw new LockAcquisitionFailedException(input.targetAccountId().toString());
            }

            Output output = transactionTemplate.execute(status -> executeTransfer(input));
            if (output == null) {
                throw new IllegalStateException("Transfer transaction returned null output");
            }
            return output;
        } finally {
            // Step 5: Release distributed locks
            if (secondLockAcquired) releaseLockSafely(secondLockKey);
            if (firstLockAcquired) releaseLockSafely(firstLockKey);
        }
    }

    private boolean isTransientConcurrencyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("40001".equals(sqlState) || "40P01".equals(sqlState)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry() {
        if (transferRetryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(transferRetryBackoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry transfer", e);
        }
    }

    protected Output executeTransfer(Input input) {
        // Step 4.1: SELECT FOR UPDATE (in UUID lexicographic order for consistency)
        UUID firstId = input.sourceAccountId().toString().compareTo(input.targetAccountId().toString()) <= 0
                ? input.sourceAccountId() : input.targetAccountId();
        UUID secondId = firstId.equals(input.sourceAccountId()) ? input.targetAccountId() : input.sourceAccountId();

        accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account source = accountRepository.findById(input.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(input.sourceAccountId()));
        Account target = accountRepository.findById(input.targetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(input.targetAccountId()));

        // Step 4.2: Business rule validations
        if (!source.canSend()) {
            throw new InactiveSourceAccountException(source.getId(), source.getStatus());
        }
        if (!target.canReceive()) {
            throw new InactiveTargetAccountException(target.getId(), target.getStatus());
        }
        if (source.getCurrency() != target.getCurrency()) {
            throw new CurrencyMismatchException(source.getCurrency(), target.getCurrency());
        }
        Money amount = Money.of(input.amount(), source.getCurrency());
        if (!Money.of(source.getBalance(), source.getCurrency()).isGreaterThanOrEqual(amount)) {
            throw new InsufficientBalanceException(source.getId(), source.getBalance(), input.amount());
        }

        // Step 4.3: Create transaction record (status: PROCESSING)
        Transaction tx = Transaction.create(input.idempotencyKey(), source.getId(), target.getId(),
                input.amount(), source.getCurrency(), input.description(), input.metadata());
        tx = transactionRepository.save(tx);

        // Step 4.4: Create DEBIT ledger entry
        BigDecimal sourceBalanceBefore = source.getBalance();
        source.debit(amount);
        BigDecimal sourceBalanceAfter = source.getBalance();
        LedgerEntry debit = new LedgerEntry(tx.getId(), source.getId(), EntryType.DEBIT,
                input.amount(), sourceBalanceBefore, sourceBalanceAfter);
        ledgerEntryRepository.save(debit);

        // Step 4.5: Create CREDIT ledger entry
        BigDecimal targetBalanceBefore = target.getBalance();
        target.credit(amount);
        BigDecimal targetBalanceAfter = target.getBalance();
        LedgerEntry credit = new LedgerEntry(tx.getId(), target.getId(), EntryType.CREDIT,
                input.amount(), targetBalanceBefore, targetBalanceAfter);
        ledgerEntryRepository.save(credit);

        // Step 4.6–4.7: Persist updated balances
        accountRepository.save(source);
        accountRepository.save(target);

        // Step 4.8: Mark transaction COMPLETED
        tx.complete();
        tx = transactionRepository.update(tx);

        return new Output(
                tx.getId(),
                tx.getIdempotencyKey(),
                source.getId(),
                target.getId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                sourceBalanceAfter,
                targetBalanceAfter,
                tx.getProcessedAt(),
                false
        );
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        try {
            UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new MissingIdempotencyKeyException();
        }
    }

    private String getLockKey(UUID sourceId, UUID targetId, boolean first) {
        int cmp = sourceId.toString().compareTo(targetId.toString());
        UUID minId = cmp <= 0 ? sourceId : targetId;
        UUID maxId = cmp <= 0 ? targetId : sourceId;
        return "vcb:lock:" + (first ? minId : maxId);
    }

    private boolean acquireLockSafely(String lockKey) {
        try {
            return distributedLockProvider.acquireLock(lockKey, 10);
        } catch (Exception e) {
            log.warn("Failed to acquire distributed lock '{}', proceeding with DB lock only: {}",
                    lockKey, e.getMessage());
            return true; // Graceful degradation
        }
    }

    private void releaseLockSafely(String lockKey) {
        try {
            distributedLockProvider.releaseLock(lockKey);
        } catch (Exception e) {
            log.warn("Failed to release distributed lock '{}': {}", lockKey, e.getMessage());
        }
    }

    private void cacheResponse(String idempotencyKey, Output output) {
        try {
            String json = objectMapper.writeValueAsString(output);
            idempotencyProvider.markCompleted(idempotencyKey, json);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency response for key '{}': {}", idempotencyKey, e.getMessage());
        }
    }

    private void publishEventSafely(Output output) {
        try {
            eventPublisher.publish(new TransactionCompletedEvent(
                    UUID.randomUUID().toString(),
                    "TRANSACTION_COMPLETED",
                    output.transactionId(),
                    output.sourceAccountId(),
                    output.targetAccountId(),
                    output.amount(),
                    output.currency(),
                    output.processedAt(),
                    Map.of()
            ));
        } catch (Exception e) {
            metrics.recordRabbitMQPublishFailure();
            log.error("Failed to publish transaction event for transactionId={}: {}",
                    output.transactionId(), e.getMessage());
        }
    }

    private Output deserializeOutput(String json) {
        try {
            return objectMapper.readValue(json, Output.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached idempotency response: {}", e.getMessage());
            throw new RuntimeException("Failed to replay cached response", e);
        }
    }

    public record Input(
            String idempotencyKey,
            UUID sourceAccountId,
            UUID targetAccountId,
            BigDecimal amount,
            String description,
            Map<String, Object> metadata
    ) {}

    public record Output(
            UUID transactionId,
            String idempotencyKey,
            UUID sourceAccountId,
            UUID targetAccountId,
            BigDecimal amount,
            Currency currency,
            TransactionStatus status,
            BigDecimal sourceBalanceAfter,
            BigDecimal targetBalanceAfter,
            Instant processedAt,
            boolean replayed
    ) {
        public Output asReplay() {
            return new Output(transactionId, idempotencyKey, sourceAccountId, targetAccountId,
                    amount, currency, status, sourceBalanceAfter, targetBalanceAfter, processedAt, true);
        }
    }
}
