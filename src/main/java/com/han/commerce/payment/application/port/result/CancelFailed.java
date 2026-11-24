package com.han.commerce.payment.application.port.result;

public final class CancelFailed implements CancelResult {

    private final String reason;

    public CancelFailed(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}