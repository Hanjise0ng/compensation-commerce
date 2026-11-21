package com.han.commerce.payment.application;

import java.time.LocalDateTime;

public final class Canceled implements CancelResult {

    private final LocalDateTime canceledAt;

    public Canceled(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

}