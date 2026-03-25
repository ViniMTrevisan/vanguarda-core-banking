package com.vinicius.vanguarda.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetrics {

    private final Counter transactionCompletedCounter;
    private final Counter transactionFailedCounter;
    private final Counter idempotencyReplayedCounter;
    private final Counter idempotencyNonReplayedCounter;
    private final Counter lockFailureCounter;
    private final Counter rabbitPublishFailureCounter;
    private final Timer transactionTimer;
    private final Timer balanceQueryTimer;

    public TransactionMetrics(MeterRegistry registry) {
        this.transactionCompletedCounter = Counter.builder("vcb.transactions.total")
                .tag("status", "COMPLETED")
                .description("Total completed transactions")
                .register(registry);
        this.transactionFailedCounter = Counter.builder("vcb.transactions.total")
                .tag("status", "FAILED")
                .description("Total failed transactions")
                .register(registry);
        this.idempotencyReplayedCounter = Counter.builder("vcb.idempotency.hits")
                .tag("replayed", "true")
                .description("Idempotency cache hits (replayed)")
                .register(registry);
        this.idempotencyNonReplayedCounter = Counter.builder("vcb.idempotency.hits")
                .tag("replayed", "false")
                .description("Idempotency cache hits (processing conflict)")
                .register(registry);
        this.lockFailureCounter = Counter.builder("vcb.distributed.lock.failures")
                .description("Distributed lock acquisition failures")
                .register(registry);
        this.rabbitPublishFailureCounter = Counter.builder("vcb.rabbitmq.publish.failures")
                .description("RabbitMQ event publish failures")
                .register(registry);
        this.transactionTimer = Timer.builder("vcb.transactions.duration")
                .description("TransferMoneyUseCase execution duration")
                .register(registry);
        this.balanceQueryTimer = Timer.builder("vcb.balance.query.duration")
                .description("Balance query execution duration")
                .register(registry);
    }

    public void recordTransaction(String status) {
        if ("COMPLETED".equals(status)) {
            transactionCompletedCounter.increment();
        } else {
            transactionFailedCounter.increment();
        }
    }

    public void recordIdempotencyHit(boolean replayed) {
        if (replayed) {
            idempotencyReplayedCounter.increment();
        } else {
            idempotencyNonReplayedCounter.increment();
        }
    }

    public void recordLockFailure() {
        lockFailureCounter.increment();
    }

    public void recordRabbitMQPublishFailure() {
        rabbitPublishFailureCounter.increment();
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start();
    }

    public void stopTransactionTimer(Timer.Sample sample) {
        sample.stop(transactionTimer);
    }

    public Timer.Sample startBalanceQueryTimer() {
        return Timer.start();
    }

    public void stopBalanceQueryTimer(Timer.Sample sample) {
        sample.stop(balanceQueryTimer);
    }
}
