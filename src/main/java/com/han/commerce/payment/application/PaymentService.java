package com.han.commerce.payment.application;

import com.han.commerce.common.domain.Money;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.payment.application.dto.RefundInfo;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import com.han.commerce.payment.domain.refund.Refund;
import com.han.commerce.payment.domain.refund.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public PaymentService(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    public void prepare(Long orderId, Money amount) {
        paymentRepository.save(Payment.ready(orderId, amount));
    }

    public void attempt(Long orderId, String paymentKey) {
        if (paymentRepository.existsByPaymentKey(paymentKey)) {
            throw new DomainException(ErrorCode.PAYMENT_KEY_ALREADY_USED);
        }
        findPayment(orderId).attempt(paymentKey);
    }

    public void approve(Long orderId, String method, LocalDateTime approvedAt) {
        findPayment(orderId).approve(method, approvedAt);
    }

    public void fail(Long orderId, String failureCode, String failureMessage) {
        findPayment(orderId).fail(failureCode, failureMessage);
    }

    public void requestRefund(Long orderId, String paymentKey, Money amount, String reason, LocalDateTime now) {
        refundRepository.save(Refund.request(orderId, paymentKey, amount, reason, now));
    }

    public RefundInfo getRefund(Long orderId) {
        return RefundInfo.from(findRefund(orderId));
    }

    public void completeCancel(Long orderId, LocalDateTime canceledAt) {
        findPayment(orderId).cancel(canceledAt);
        findRefund(orderId).complete(canceledAt);
    }

    private Payment findPayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private Refund findRefund(Long orderId) {
        return refundRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.REFUND_NOT_FOUND));
    }

}