package com.han.commerce.payment.domain;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class RefundTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 12, 0);

    @Test
    @DisplayName("환불을 요청하면 요청 상태이고 완료 시각은 비어 있다")
    void request() {
        Refund refund = Refund.request(1L, "pay-key-1", Money.of(14_000), "주문 확정 실패", NOW);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.getAmount()).isEqualTo(Money.of(14_000));
        assertThat(refund.getRequestedAt()).isEqualTo(NOW);
        assertThat(refund.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("완료하면 완료 시각이 남고 다시 완료할 수 없다")
    void complete() {
        Refund refund = Refund.request(1L, "pay-key-1", Money.of(14_000), "주문 확정 실패", NOW);

        refund.complete(NOW.plusSeconds(5));

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getCompletedAt()).isEqualTo(NOW.plusSeconds(5));
        assertErrorCode(() -> refund.complete(NOW.plusSeconds(9)), ErrorCode.INVALID_REFUND_STATE);
    }

}