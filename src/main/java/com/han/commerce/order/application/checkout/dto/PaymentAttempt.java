package com.han.commerce.order.application.checkout.dto;

import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.application.port.request.PaymentConfirmRequest;

public class PaymentAttempt {

    private final Long orderId;
    private final String orderNo;
    private final String paymentKey;
    private final Money payAmount;

    public PaymentAttempt(Long orderId, String orderNo, String paymentKey, Money payAmount) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.paymentKey = paymentKey;
        this.payAmount = payAmount;
    }

    public PaymentConfirmRequest toRequest() {
        return new PaymentConfirmRequest(orderNo, paymentKey, payAmount);
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

}