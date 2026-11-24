package com.han.commerce.order.application.recovery.dto;

public enum RecoveryOutcome {
    CONFIRMED,
    COMPENSATED,
    POSTPONED,
    SKIPPED,
    CANCELED,
    CANCEL_POSTPONED,
    CANCEL_GAVE_UP
}