package com.han.commerce.payment.domain.refund;

import com.han.commerce.common.domain.entity.BaseEntity;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund")
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String paymentKey;

    @Embedded
    private Money amount;

    private String reason;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    protected Refund() {
    }

    public static Refund request(Long orderId, String paymentKey, Money amount, String reason, LocalDateTime now) {
        Refund refund = new Refund();
        refund.orderId = orderId;
        refund.paymentKey = paymentKey;
        refund.amount = amount;
        refund.reason = reason;
        refund.status = RefundStatus.REQUESTED;
        refund.requestedAt = now;
        return refund;
    }

    public void complete(LocalDateTime now) {
        if (status != RefundStatus.REQUESTED) {
            throw new DomainException(ErrorCode.INVALID_REFUND_STATE, "현재 " + status);
        }
        this.status = RefundStatus.COMPLETED;
        this.completedAt = now;
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

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

}