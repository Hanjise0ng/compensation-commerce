package com.han.commerce.payment.application;

import com.han.commerce.common.domain.Money;

public class PaymentCancelRequest {

    private final String paymentKey;
    private final Money amount;
    private final String reason;

    public PaymentCancelRequest(String paymentKey, Money amount, String reason) {
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.reason = reason;
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

}