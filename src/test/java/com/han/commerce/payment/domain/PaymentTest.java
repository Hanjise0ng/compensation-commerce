package com.han.commerce.payment.domain;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 12, 0);

    @Test
    @DisplayName("결제는 READY로 시작해 승인된다")
    void approve() {
        Payment payment = Payment.ready(100L, Money.of(20_000));

        payment.attempt("pay-key-1");
        payment.approve("CARD", NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPaymentKey()).isEqualTo("pay-key-1");
        assertThat(payment.getApprovedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("실패한 결제는 승인할 수 없다")
    void cannotApproveAfterFail() {
        Payment payment = Payment.ready(100L, Money.of(20_000));
        payment.fail("REJECT_CARD", "한도 초과");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertErrorCode(() -> payment.approve("CARD", NOW), ErrorCode.INVALID_PAYMENT_STATE);
    }

    @Test
    @DisplayName("READY와 APPROVED 결제만 취소할 수 있다")
    void cancel() {
        Payment approved = Payment.ready(100L, Money.of(20_000));
        approved.approve("CARD", NOW);
        approved.cancel(NOW.plusMinutes(1));
        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.CANCELED);

        Payment failed = Payment.ready(101L, Money.of(20_000));
        failed.fail("REJECT_CARD", "한도 초과");
        assertErrorCode(() -> failed.cancel(NOW), ErrorCode.INVALID_PAYMENT_STATE);
    }

}