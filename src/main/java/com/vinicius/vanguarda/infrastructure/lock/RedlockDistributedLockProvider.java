package com.vinicius.vanguarda.infrastructure.lock;

import com.vinicius.vanguarda.domain.service.DistributedLockProvider;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedlockDistributedLockProvider implements DistributedLockProvider {

    private static final Logger log = LoggerFactory.getLogger(RedlockDistributedLockProvider.class);

    private final RedissonClient redissonClient;
    private final int retryAttempts;
    private final long retryIntervalMs;
    private final ThreadLocal<Map<String, RLock>> acquiredLocks = ThreadLocal.withInitial(HashMap::new);

    public RedlockDistributedLockProvider(RedissonClient redissonClient,
                                          @Value("${vcb.distributed-lock.retry-attempts:3}") int retryAttempts,
                                          @Value("${vcb.distributed-lock.retry-interval-ms:100}") long retryIntervalMs) {
        this.redissonClient = redissonClient;
        this.retryAttempts = retryAttempts;
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public boolean acquireLock(String lockKey, long ttlSeconds) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            for (int attempt = 0; attempt <= retryAttempts; attempt++) {
                boolean acquired = lock.tryLock(0, ttlSeconds, TimeUnit.SECONDS);
                if (acquired) {
                    acquiredLocks.get().put(lockKey, lock);
                    return true;
                }
                if (attempt < retryAttempts) {
                    Thread.sleep(retryIntervalMs);
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while acquiring distributed lock '{}': {}", lockKey, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error acquiring distributed lock '{}', proceeding without distributed lock: {}",
                    lockKey, e.getMessage());
            return true; // Graceful degradation: proceed with DB lock only
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        try {
            RLock lock = acquiredLocks.get().remove(lockKey);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("Error releasing distributed lock '{}': {}", lockKey, e.getMessage());
        } finally {
            if (acquiredLocks.get().isEmpty()) {
                acquiredLocks.remove();
            }
        }
    }
}
