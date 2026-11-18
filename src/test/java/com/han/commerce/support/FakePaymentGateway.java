package com.han.commerce.support;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.payment.application.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Primary
public class FakePaymentGateway implements PaymentGateway {

    private final ClockHolder clockHolder;
    private final AtomicInteger callCount = new AtomicInteger();

    private volatile PaymentResult scripted;
    private volatile long delayMs;

    public FakePaymentGateway(ClockHolder clockHolder) {
        this.clockHolder = clockHolder;
    }

    public void willApprove() {
        this.scripted = null;
    }

    public void willDecline(String code, String message) {
        this.scripted = new Declined(code, message);
    }

    public void willTimeout() {
        this.scripted = new Unknown("read timeout");
    }

    public void delay(long delayMs) {
        this.delayMs = delayMs;
    }

    public void reset() {
        this.scripted = null;
        this.delayMs = 0;
        this.callCount.set(0);
    }

    public int callCount() {
        return callCount.get();
    }

    @Override
    public PaymentResult confirm(PaymentConfirmRequest request) {
        callCount.incrementAndGet();
        sleep();
        if (scripted != null) {
            return scripted;
        }
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