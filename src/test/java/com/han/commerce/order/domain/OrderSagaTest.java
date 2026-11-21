package com.han.commerce.order.domain;

import com.han.commerce.common.domain.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class OrderSagaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 12, 0);

    private OrderSaga startedSaga() {
        return OrderSaga.start(1L, "pay-key-1", NOW, NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("사가를 시작하면 진행 중 상태이고 첫 확인 시각이 정해진다")
    void start() {
        OrderSaga saga = startedSaga();

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.isInProgress()).isTrue();
        assertThat(saga.getAttemptCount()).isEqualTo(0);
        assertThat(saga.getNextCheckAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("완료하면 더 이상 확인 대상이 아니다")
    void complete() {
        OrderSaga saga = startedSaga();

        saga.complete(NOW.plusSeconds(3));

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.getNextCheckAt()).isNull();
        assertThat(saga.getFinishedAt()).isEqualTo(NOW.plusSeconds(3));
    }

    @Test
    @DisplayName("이미 끝난 사가는 다시 끝낼 수 없다")
    void cannotFinishTwice() {
        OrderSaga saga = startedSaga();
        saga.complete(NOW);

        assertErrorCode(() -> saga.compensated(NOW), ErrorCode.INVALID_SAGA_STATE);
        assertErrorCode(() -> saga.complete(NOW), ErrorCode.INVALID_SAGA_STATE);
    }

    @Test
    @DisplayName("미루면 시도 횟수가 오르고 다음 확인 시각이 바뀐다")
    void postpone() {
        OrderSaga saga = startedSaga();

        saga.postpone("read timeout", NOW.plusMinutes(5));

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.getAttemptCount()).isEqualTo(1);
        assertThat(saga.getLastError()).isEqualTo("read timeout");
        assertThat(saga.getNextCheckAt()).isEqualTo(NOW.plusMinutes(5));
    }

}