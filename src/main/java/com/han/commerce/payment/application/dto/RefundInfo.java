package com.han.commerce.payment.application.dto;

import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.domain.refund.Refund;

public class RefundInfo {

    private final String paymentKey;
    private final Money amount;
    private final String reason;

    private RefundInfo(String paymentKey, Money amount, String reason) {
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.reason = reason;
    }

    public static RefundInfo from(Refund refund) {
        return new RefundInfo(refund.getPaymentKey(), refund.getAmount(), refund.getReason());
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