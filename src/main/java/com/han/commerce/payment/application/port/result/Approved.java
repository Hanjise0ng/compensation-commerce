package com.han.commerce.payment.application.port.result;

import com.han.commerce.common.domain.Money;
import java.time.LocalDateTime;

public final class Approved implements PaymentResult {

    private final String paymentKey;
    private final Money approvedAmount;
    private final String method;
    private final LocalDateTime approvedAt;

    public Approved(String paymentKey, Money approvedAmount, String method, LocalDateTime approvedAt) {
        this.paymentKey = paymentKey;
        this.approvedAmount = approvedAmount;
        this.method = method;
        this.approvedAt = approvedAt;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public Money getApprovedAmount() {
        return approvedAmount;
    }

    public String getMethod() {
        return method;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

}