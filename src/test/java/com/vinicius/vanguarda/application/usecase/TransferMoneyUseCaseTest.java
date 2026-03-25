package com.vinicius.vanguarda.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinicius.vanguarda.domain.exception.*;
import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.Transaction;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import com.vinicius.vanguarda.domain.repository.LedgerEntryRepository;
import com.vinicius.vanguarda.domain.repository.TransactionRepository;
import com.vinicius.vanguarda.domain.service.DistributedLockProvider;
import com.vinicius.vanguarda.domain.service.EventPublisher;
import com.vinicius.vanguarda.domain.service.IdempotencyProvider;
import com.vinicius.vanguarda.infrastructure.metrics.TransactionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransferMoneyUseCaseTest {

    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock IdempotencyProvider idempotencyProvider;
    @Mock DistributedLockProvider distributedLockProvider;
    @Mock EventPublisher eventPublisher;
    @Mock PlatformTransactionManager transactionManager;

    private TransferMoneyUseCase useCase;
    private TransactionMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new TransactionMetrics(new SimpleMeterRegistry());
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().doNothing().when(transactionManager).commit(any(org.springframework.transaction.TransactionStatus.class));
        lenient().doNothing().when(transactionManager).rollback(any(org.springframework.transaction.TransactionStatus.class));
        useCase = new TransferMoneyUseCase(
                accountRepository, transactionRepository, ledgerEntryRepository,
                idempotencyProvider, distributedLockProvider, eventPublisher,
                metrics, mapper, transactionManager, 5, 1);
    }

    @Test
    void shouldThrowOnMissingIdempotencyKey() {
        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                null, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null, null);
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MissingIdempotencyKeyException.class);
    }

    @Test
    void shouldThrowOnInvalidIdempotencyKeyFormat() {
        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                "not-a-uuid", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null, null);
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MissingIdempotencyKeyException.class);
    }

    @Test
    void shouldReturnCachedResponseOnReplay() {
        String key = UUID.randomUUID().toString();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String cachedJson = buildCachedJson(key, sourceId, targetId);

        when(idempotencyProvider.getCompletedResponse(key)).thenReturn(Optional.of(cachedJson));

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                key, sourceId, targetId, new BigDecimal("100.00"), null, null);
        TransferMoneyUseCase.Output output = useCase.execute(input);

        assertThat(output.replayed()).isTrue();
        verifyNoInteractions(transactionRepository, ledgerEntryRepository);
    }

    @Test
    void shouldThrowConflictWhenProcessing() {
        String key = UUID.randomUUID().toString();
        when(idempotencyProvider.getCompletedResponse(key)).thenReturn(Optional.empty());
        when(idempotencyProvider.isProcessing(key)).thenReturn(true);

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                key, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null, null);
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(TransactionInProgressException.class);
    }

    @Test
    void shouldThrowWhenLockCannotBeAcquired() {
        String key = UUID.randomUUID().toString();
        when(idempotencyProvider.getCompletedResponse(key)).thenReturn(Optional.empty());
        when(idempotencyProvider.isProcessing(key)).thenReturn(false);
        when(distributedLockProvider.acquireLock(anyString(), anyLong())).thenReturn(false);

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                key, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), null, null);
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(LockAcquisitionFailedException.class);
    }

    @Test
    void shouldProceedGracefullyWhenRedisUnavailableForLocking() {
        String key = UUID.randomUUID().toString();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("500.00"));
        Account target = Account.create("o2", "Bob", Currency.BRL, new BigDecimal("200.00"));

        when(idempotencyProvider.getCompletedResponse(key)).thenReturn(Optional.empty());
        when(idempotencyProvider.isProcessing(key)).thenReturn(false);
        // Simulate Redis down — acquireLock throws
        when(distributedLockProvider.acquireLock(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        // With graceful degradation, acquireLockSafely returns true on exception
        // So execution should proceed to the DB transaction
        when(accountRepository.findByIdForUpdate(any())).thenReturn(Optional.of(source));
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));
        Transaction tx = Transaction.create(key, sourceId, targetId,
                new BigDecimal("100.00"), Currency.BRL, null, null);
        when(transactionRepository.save(any())).thenReturn(tx);
        when(transactionRepository.update(any())).thenReturn(tx);
        when(ledgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                key, sourceId, targetId, new BigDecimal("100.00"), null, null);

        // Should not throw — graceful degradation proceeds with DB lock only
        assertThatCode(() -> useCase.execute(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowWhenRabbitMQPublishFails() {
        String key = UUID.randomUUID().toString();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("500.00"));
        Account target = Account.create("o2", "Bob", Currency.BRL, new BigDecimal("200.00"));

        when(idempotencyProvider.getCompletedResponse(key)).thenReturn(Optional.empty());
        when(idempotencyProvider.isProcessing(key)).thenReturn(false);
        when(distributedLockProvider.acquireLock(anyString(), anyLong())).thenReturn(true);
        when(accountRepository.findByIdForUpdate(any())).thenReturn(Optional.of(source));
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));
        Transaction tx = Transaction.create(key, sourceId, targetId,
                new BigDecimal("100.00"), Currency.BRL, null, null);
        when(transactionRepository.save(any())).thenReturn(tx);
        when(transactionRepository.update(any())).thenReturn(tx);
        when(ledgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publish(any());

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                key, sourceId, targetId, new BigDecimal("100.00"), null, null);
        assertThatCode(() -> useCase.execute(input)).doesNotThrowAnyException();
    }

    private String buildCachedJson(String key, UUID sourceId, UUID targetId) {
        return String.format(
                "{\"transactionId\":\"%s\",\"idempotencyKey\":\"%s\"," +
                "\"sourceAccountId\":\"%s\",\"targetAccountId\":\"%s\"," +
                "\"amount\":100.00,\"currency\":\"BRL\",\"status\":\"COMPLETED\"," +
                "\"sourceBalanceAfter\":400.00,\"targetBalanceAfter\":300.00," +
                "\"processedAt\":\"2024-01-01T00:00:00Z\",\"replayed\":false}",
                UUID.randomUUID(), key, sourceId, targetId);
    }
}
