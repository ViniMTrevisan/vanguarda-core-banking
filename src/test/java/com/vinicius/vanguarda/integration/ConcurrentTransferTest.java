package com.vinicius.vanguarda.integration;

import com.vinicius.vanguarda.application.usecase.CreateAccountUseCase;
import com.vinicius.vanguarda.application.usecase.GetAccountUseCase;
import com.vinicius.vanguarda.application.usecase.TransferMoneyUseCase;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentTransferTest extends AbstractIntegrationTest {

    @Autowired CreateAccountUseCase createAccountUseCase;
    @Autowired TransferMoneyUseCase transferMoneyUseCase;
    @Autowired GetAccountUseCase getAccountUseCase;

    /**
     * TC-01: 100 threads attempting to debit the same source account.
     * Final balance must be mathematically correct and never negative.
     */
    @Test
    void tc01_hundredConcurrentDebitsShouldProduceConsistentFinalBalance() throws InterruptedException {
        int threads = 100;
        BigDecimal transferAmount = new BigDecimal("10.00");
        BigDecimal initialBalance = new BigDecimal("500.00"); // Only 50 transfers should succeed

        var source = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "concOwner1", "Concurrent Source", Currency.BRL, initialBalance));
        var target = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "concOwner2", "Concurrent Target", Currency.BRL, BigDecimal.ZERO));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String key = UUID.randomUUID().toString();
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                            key, source.accountId(), target.accountId(),
                            transferAmount, "Concurrent test", null));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
                return null;
            }));
        }

        latch.countDown(); // Release all threads simultaneously
        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        // Expected: exactly 50 successes (500 / 10), 50 failures (insufficient balance)
        GetAccountUseCase.Output sourceState = getAccountUseCase.execute(source.accountId());
        GetAccountUseCase.Output targetState = getAccountUseCase.execute(target.accountId());

        // Final balance must never be negative
        assertThat(sourceState.balance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Source + Target balances must equal total initial balance (conservation of money)
        BigDecimal totalBalance = sourceState.balance().add(targetState.balance());
        assertThat(totalBalance).isEqualByComparingTo(initialBalance);

        // Exactly 50 threads should have succeeded
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);
    }

    /**
     * TC-02: Bidirectional transfers (A→B and B→A) concurrently.
     * Both must complete without deadlock.
     */
    @Test
    void tc02_bidirectionalTransfersShouldCompleteWithoutDeadlock() throws Exception {
        var accountA = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "biOwner1", "Account A", Currency.BRL, new BigDecimal("1000.00")));
        var accountB = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "biOwner2", "Account B", Currency.BRL, new BigDecimal("1000.00")));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<TransferMoneyUseCase.Output> aToB = executor.submit(() -> {
            latch.await();
            return transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                    UUID.randomUUID().toString(),
                    accountA.accountId(), accountB.accountId(),
                    new BigDecimal("100.00"), "A→B", null));
        });

        Future<TransferMoneyUseCase.Output> bToA = executor.submit(() -> {
            latch.await();
            return transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                    UUID.randomUUID().toString(),
                    accountB.accountId(), accountA.accountId(),
                    new BigDecimal("200.00"), "B→A", null));
        });

        latch.countDown();
        executor.shutdown();

        // Both transfers must complete without timeout (no deadlock)
        TransferMoneyUseCase.Output abResult = aToB.get(15, TimeUnit.SECONDS);
        TransferMoneyUseCase.Output baResult = bToA.get(15, TimeUnit.SECONDS);

        assertThat(abResult).isNotNull();
        assertThat(baResult).isNotNull();

        // Conservation: A had 1000, sent 100, received 200 → 1100
        // B had 1000, received 100, sent 200 → 900
        GetAccountUseCase.Output stateA = getAccountUseCase.execute(accountA.accountId());
        GetAccountUseCase.Output stateB = getAccountUseCase.execute(accountB.accountId());

        BigDecimal total = stateA.balance().add(stateB.balance());
        assertThat(total).isEqualByComparingTo("2000.00");
        assertThat(stateA.balance()).isEqualByComparingTo("1100.00");
        assertThat(stateB.balance()).isEqualByComparingTo("900.00");
    }
}
