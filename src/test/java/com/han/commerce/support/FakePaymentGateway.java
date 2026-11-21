package com.han.commerce.support;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.application.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Primary
public class FakePaymentGateway implements PaymentGateway {

    private final ClockHolder clockHolder;
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicInteger inquiryCount = new AtomicInteger();
    private volatile PaymentResult scripted;
    private volatile PaymentResult inquiryScripted;
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

    public void willApproveAmount(long amount) {
        this.scripted = new Approved("pay-key-1", Money.of(amount), "CARD", clockHolder.now());
    }

    public void inquiryWillFindApproved() {
        this.inquiryScripted = null;
    }

    public void inquiryWillFindNothing() {
        this.inquiryScripted = new Declined("PAYMENT_NOT_FOUND", "PG에 결제 기록이 없습니다.");
    }

    public void inquiryWillTimeout() {
        this.inquiryScripted = new Unknown("inquiry timeout");
    }

    public void delay(long delayMs) {
        this.delayMs = delayMs;
    }

    public void reset() {
        this.scripted = null;
        this.inquiryScripted = null;
        this.delayMs = 0;
        this.callCount.set(0);
        this.inquiryCount.set(0);
    }

    public int callCount() {
        return callCount.get();
    }

    public int inquiryCount() {
        return inquiryCount.get();
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

    @Override
    public PaymentResult inquire(PaymentInquiryRequest request) {
        inquiryCount.incrementAndGet();
        if (inquiryScripted != null) {
            return inquiryScripted;
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