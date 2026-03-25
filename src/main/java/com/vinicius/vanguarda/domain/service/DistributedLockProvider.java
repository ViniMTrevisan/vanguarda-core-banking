package com.vinicius.vanguarda.domain.service;

public interface DistributedLockProvider {

    boolean acquireLock(String lockKey, long ttlSeconds);

    void releaseLock(String lockKey);
}
