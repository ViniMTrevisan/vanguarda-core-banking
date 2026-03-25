package com.vinicius.vanguarda.infrastructure.cache;

import com.vinicius.vanguarda.domain.service.IdempotencyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisIdempotencyProvider implements IdempotencyProvider {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyProvider.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final String PROCESSING_VALUE = "PROCESSING";
    private static final String COMPLETED_PREFIX = "COMPLETED:";

    private final StringRedisTemplate redisTemplate;
    private final long processingTtlSeconds;
    private final long completedTtlHours;

    public RedisIdempotencyProvider(StringRedisTemplate redisTemplate,
                                     @Value("${vcb.idempotency.processing-ttl-seconds:30}") long processingTtlSeconds,
                                     @Value("${vcb.idempotency.ttl-hours:24}") long completedTtlHours) {
        this.redisTemplate = redisTemplate;
        this.processingTtlSeconds = processingTtlSeconds;
        this.completedTtlHours = completedTtlHours;
    }

    @Override
    public Optional<String> getCompletedResponse(String key) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
            if (value != null && value.startsWith(COMPLETED_PREFIX)) {
                return Optional.of(value.substring(COMPLETED_PREFIX.length()));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis unavailable when checking completed idempotency key '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isProcessing(String key) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
            return PROCESSING_VALUE.equals(value);
        } catch (Exception e) {
            log.warn("Redis unavailable when checking processing idempotency key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public void markProcessing(String key) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + key, PROCESSING_VALUE,
                    Duration.ofSeconds(processingTtlSeconds));
        } catch (Exception e) {
            log.warn("Redis unavailable when marking idempotency key '{}' as PROCESSING: {}", key, e.getMessage());
        }
    }

    @Override
    public void markCompleted(String key, String responseJson) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + key, COMPLETED_PREFIX + responseJson,
                    Duration.ofHours(completedTtlHours));
        } catch (Exception e) {
            log.warn("Redis unavailable when marking idempotency key '{}' as COMPLETED: {}", key, e.getMessage());
        }
    }
}
