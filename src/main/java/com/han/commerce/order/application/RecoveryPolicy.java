package com.han.commerce.order.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RecoveryPolicy {

    private final Duration recoverAfter;

    public RecoveryPolicy(@Value("${commerce.payment.recover-after:PT1M}") Duration recoverAfter) {
        this.recoverAfter = recoverAfter;
    }

    public Duration firstCheckDelay() {
        return recoverAfter;
    }

    public Duration retryDelay(int attemptCount) {
        return recoverAfter;
    }

}