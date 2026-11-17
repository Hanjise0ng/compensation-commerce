package com.han.commerce.order.presentation;

import com.han.commerce.order.application.ConfirmPaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class ConfirmPaymentRequest {

    @NotBlank
    private String orderNo;

    @NotBlank
    private String paymentKey;

    @PositiveOrZero
    private long amount;

    protected ConfirmPaymentRequest() {
    }

    public ConfirmPaymentCommand toCommand(Long memberId) {
        return new ConfirmPaymentCommand(memberId, orderNo, paymentKey, amount);
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