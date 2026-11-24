package com.han.commerce.order.application.compensation.dto;

import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.application.port.request.PaymentCancelRequest;

public class CancelTarget {

    private final Long sagaId;
    private final Long orderId;
    private final String paymentKey;
    private final Money amount;
    private final String reason;

    public CancelTarget(Long sagaId, Long orderId, String paymentKey, Money amount, String reason) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.reason = reason;
    }

    public PaymentCancelRequest toCancelRequest() {
        return new PaymentCancelRequest(paymentKey, amount, reason);
    }

    public Long getSagaId() {
        return sagaId;
    }

    public Long getOrderId() {
        return orderId;
    }

}