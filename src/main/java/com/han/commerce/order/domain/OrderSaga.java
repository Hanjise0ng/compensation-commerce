package com.han.commerce.order.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_saga")
public class OrderSaga extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    private int attemptCount;

    private LocalDateTime nextCheckAt;

    private String lastError;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Version
    private Long version;

    protected OrderSaga() {
    }

    public static OrderSaga start(Long orderId, String paymentKey, LocalDateTime now, LocalDateTime firstCheckAt) {
        OrderSaga saga = new OrderSaga();
        saga.orderId = orderId;
        saga.paymentKey = paymentKey;
        saga.status = SagaStatus.STARTED;
        saga.attemptCount = 0;
        saga.startedAt = now;
        saga.nextCheckAt = firstCheckAt;
        return saga;
    }

    public void complete(LocalDateTime now) {
        requireStatus(SagaStatus.STARTED);
        finish(SagaStatus.COMPLETED, now);
    }

    public void compensated(LocalDateTime now) {
        requireStatus(SagaStatus.STARTED);
        finish(SagaStatus.COMPENSATED, now);
    }

    public void postpone(String reason, LocalDateTime nextCheckAt) {
        requireStatus(SagaStatus.STARTED);
        this.attemptCount++;
        this.lastError = truncate(reason);
        this.nextCheckAt = nextCheckAt;
    }

    public boolean isInProgress() {
        return status == SagaStatus.STARTED;
    }

    private void finish(SagaStatus finalStatus, LocalDateTime now) {
        this.status = finalStatus;
        this.finishedAt = now;
        this.nextCheckAt = null;
    }

    private void requireStatus(SagaStatus expected) {
        if (status != expected) {
            throw new DomainException(ErrorCode.INVALID_SAGA_STATE, "주문 " + orderId + ", 현재 " + status);
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_ERROR_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextCheckAt() {
        return nextCheckAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

}