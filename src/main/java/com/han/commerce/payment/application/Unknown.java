package com.han.commerce.payment.application;

public final class Unknown implements PaymentResult {

    private final String reason;

    public Unknown(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}