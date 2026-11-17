package com.han.commerce.payment.infrastructure;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.payment.application.Approved;
import com.han.commerce.payment.application.PaymentConfirmRequest;
import com.han.commerce.payment.application.PaymentGateway;
import com.han.commerce.payment.application.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StubPaymentGateway implements PaymentGateway {

    private final ClockHolder clockHolder;
    private final long delayMs;

    public StubPaymentGateway(ClockHolder clockHolder,
                              @Value("${commerce.pg.stub-delay-ms:0}") long delayMs) {
        this.clockHolder = clockHolder;
        this.delayMs = delayMs;
    }

    @Override
    public PaymentResult confirm(PaymentConfirmRequest request) {
        sleep();
        return new Approved(request.getPaymentKey(), request.getAmount(), "CARD", clockHolder.now());
    }

    private void sleep() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}