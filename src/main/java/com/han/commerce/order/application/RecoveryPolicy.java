package com.han.commerce.order.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RecoveryPolicy {

    private final Duration recoverAfter;
    private final Duration cancelRetryBase;
    private final Duration maxDelay;
    private final int maxCancelAttempts;

    public RecoveryPolicy(@Value("${commerce.payment.recover-after:PT1M}") Duration recoverAfter,
                          @Value("${commerce.payment.cancel-retry-base:PT10S}") Duration cancelRetryBase,
                          @Value("${commerce.payment.max-retry-delay:PT10M}") Duration maxDelay,
                          @Value("${commerce.payment.max-cancel-attempts:5}") int maxCancelAttempts) {
        this.recoverAfter = recoverAfter;
        this.cancelRetryBase = cancelRetryBase;
        this.maxDelay = maxDelay;
        this.maxCancelAttempts = maxCancelAttempts;
    }

    public Duration firstCheckDelay() {
        return recoverAfter;
    }

    public Duration retryDelay(int attemptCount) {
        return backoff(recoverAfter, attemptCount);
    }

    public Duration cancelRetryDelay(int attemptCount) {
        return backoff(cancelRetryBase, attemptCount);
    }

    public boolean isCancelExhausted(int attemptCount) {
        return attemptCount >= maxCancelAttempts;
    }

    private Duration backoff(Duration base, int attemptCount) {
        int doublings = Math.min(Math.max(attemptCount - 1, 0), 20);
        Duration delay = base.multipliedBy(1L << doublings);
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }

}