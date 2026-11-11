package com.han.commerce.payment.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.Embedded;
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
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String paymentKey;

    private String method;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String failureCode;

    private String failureMessage;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    @Version
    private Long version;

    protected Payment() {
    }

    public static Payment ready(Long orderId, Money amount) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    public void attempt(String paymentKey) {
        requireStatus(PaymentStatus.READY);
        this.paymentKey = paymentKey;
    }

    public void approve(String method, LocalDateTime approvedAt) {
        requireStatus(PaymentStatus.READY);
        this.status = PaymentStatus.APPROVED;
        this.method = method;
        this.approvedAt = approvedAt;
    }

    public void fail(String failureCode, String failureMessage) {
        requireStatus(PaymentStatus.READY);
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void cancel(LocalDateTime canceledAt) {
        if (status != PaymentStatus.READY && status != PaymentStatus.APPROVED) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT_STATE, "현재 " + status);
        }
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT_STATE, "현재 " + status);
        }
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

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

}