package com.han.commerce.order.application;

import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.application.PaymentInquiryRequest;

public class RecoveryTarget {

    private final Long sagaId;
    private final Long orderId;
    private final String orderNo;
    private final String paymentKey;
    private final Money payAmount;

    public RecoveryTarget(Long sagaId, Long orderId, String orderNo, String paymentKey, Money payAmount) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.paymentKey = paymentKey;
        this.payAmount = payAmount;
    }

    public PaymentInquiryRequest toInquiryRequest() {
        return new PaymentInquiryRequest(orderNo, paymentKey, payAmount);
    }

    public Long getSagaId() {
        return sagaId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

}