package com.han.commerce.support;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.Money;
import com.han.commerce.payment.application.port.PaymentGateway;
import com.han.commerce.payment.application.port.request.PaymentCancelRequest;
import com.han.commerce.payment.application.port.request.PaymentConfirmRequest;
import com.han.commerce.payment.application.port.request.PaymentInquiryRequest;
import com.han.commerce.payment.application.port.result.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Primary
public class FakePaymentGateway implements PaymentGateway {

    private final ClockHolder clockHolder;
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicInteger inquiryCount = new AtomicInteger();
    private final AtomicInteger cancelCount = new AtomicInteger();
    private volatile PaymentResult scripted;
    private volatile PaymentResult inquiryScripted;
    private volatile String cancelFailure;
    private volatile Money lastCanceledAmount;
    private volatile long delayMs;

    public FakePaymentGateway(ClockHolder clockHolder) {
        this.clockHolder = clockHolder;
    }

    public void willApprove() {
        this.scripted = null;
    }

    public void willApproveAmount(long amount) {
        this.scripted = new Approved("pay-key-1", Money.of(amount), "CARD", clockHolder.now());
    }

    public void willDecline(String code, String message) {
        this.scripted = new Declined(code, message);
    }

    public void willTimeout() {
        this.scripted = new Unknown("read timeout");
    }

    public void inquiryWillFindApproved() {
        this.inquiryScripted = null;
    }

    public void inquiryWillFindApprovedAmount(long amount) {
        this.inquiryScripted = new Approved("pay-key-1", Money.of(amount), "CARD", clockHolder.now());
    }

    public void inquiryWillFindNothing() {
        this.inquiryScripted = new Declined("PAYMENT_NOT_FOUND", "PG에 결제 기록이 없습니다.");
    }

    public void inquiryWillTimeout() {
        this.inquiryScripted = new Unknown("inquiry timeout");
    }

    public void cancelWillSucceed() {
        this.cancelFailure = null;
    }

    public void cancelWillFail(String reason) {
        this.cancelFailure = reason;
    }

    public void delay(long delayMs) {
        this.delayMs = delayMs;
    }

    public void reset() {
        this.scripted = null;
        this.inquiryScripted = null;
        this.cancelFailure = null;
        this.lastCanceledAmount = null;
        this.delayMs = 0;
        this.callCount.set(0);
        this.inquiryCount.set(0);
        this.cancelCount.set(0);
    }

    public int callCount() {
        return callCount.get();
    }

    public int inquiryCount() {
        return inquiryCount.get();
    }

    public int cancelCount() {
        return cancelCount.get();
    }

    public Money lastCanceledAmount() {
        return lastCanceledAmount;
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

    @Override
    public CancelResult cancel(PaymentCancelRequest request) {
        cancelCount.incrementAndGet();
        lastCanceledAmount = request.getAmount();
        if (cancelFailure != null) {
            return new CancelFailed(cancelFailure);
        }
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