package com.han.commerce.order.domain;

public enum SagaStatus {
    STARTED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}