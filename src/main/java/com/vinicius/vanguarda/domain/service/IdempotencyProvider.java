package com.vinicius.vanguarda.domain.service;

import java.util.Optional;

public interface IdempotencyProvider {

    Optional<String> getCompletedResponse(String key);

    boolean isProcessing(String key);

    void markProcessing(String key);

    void markCompleted(String key, String responseJson);
}
