package com.han.commerce.payment.infrastructure.pg;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.payment.application.port.PaymentGateway;
import com.han.commerce.payment.application.port.request.PaymentCancelRequest;
import com.han.commerce.payment.application.port.request.PaymentConfirmRequest;
import com.han.commerce.payment.application.port.request.PaymentInquiryRequest;
import com.han.commerce.payment.application.port.result.Approved;
import com.han.commerce.payment.application.port.result.CancelResult;
import com.han.commerce.payment.application.port.result.Canceled;
import com.han.commerce.payment.application.port.result.PaymentResult;
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

    @Override
    public PaymentResult inquire(PaymentInquiryRequest request) {
        return new Approved(request.getPaymentKey(), request.getAmount(), "CARD", clockHolder.now());
    }

    @Override
    public CancelResult cancel(PaymentCancelRequest request) {
        return new Canceled(clockHolder.now());
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