package com.han.commerce.payment.application.port.result;

public final class Unknown implements PaymentResult {

    private final String reason;

    public Unknown(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}