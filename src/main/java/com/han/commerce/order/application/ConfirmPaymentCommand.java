package com.han.commerce.order.application;

public class ConfirmPaymentCommand {

    private final Long memberId;
    private final String orderNo;
    private final String paymentKey;
    private final long amount;

    public ConfirmPaymentCommand(Long memberId, String orderNo, String paymentKey, long amount) {
        this.memberId = memberId;
        this.orderNo = orderNo;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public long getAmount() {
        return amount;
    }

}