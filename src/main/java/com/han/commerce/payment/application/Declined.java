package com.han.commerce.payment.application;

public final class Declined implements PaymentResult {

    private final String code;
    private final String message;

    public Declined(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}