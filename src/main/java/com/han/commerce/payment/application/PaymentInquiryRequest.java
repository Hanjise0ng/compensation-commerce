package com.han.commerce.payment.application;

import com.han.commerce.common.domain.Money;

public class PaymentInquiryRequest {

    private final String orderNo;
    private final String paymentKey;
    private final Money amount;

    public PaymentInquiryRequest(String orderNo, String paymentKey, Money amount) {
        this.orderNo = orderNo;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public Money getAmount() {
        return amount;
    }

}