package com.han.commerce.order.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryPolicyTest {

    private final RecoveryPolicy policy = new RecoveryPolicy(
            Duration.ofMinutes(1), Duration.ofSeconds(10), Duration.ofMinutes(10), 5);

    @Test
    @DisplayName("취소 재시도 간격은 10초에서 시작해 두 배씩 늘어난다")
    void cancelRetryDelayDoubles() {
        assertThat(policy.cancelRetryDelay(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.cancelRetryDelay(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.cancelRetryDelay(3)).isEqualTo(Duration.ofSeconds(40));
        assertThat(policy.cancelRetryDelay(4)).isEqualTo(Duration.ofSeconds(80));
    }

    @Test
    @DisplayName("조회 재시도 간격은 1분에서 시작해 10분을 넘지 않는다")
    void retryDelayIsCapped() {
        assertThat(policy.retryDelay(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(policy.retryDelay(4)).isEqualTo(Duration.ofMinutes(8));
        assertThat(policy.retryDelay(5)).isEqualTo(Duration.ofMinutes(10));
        assertThat(policy.retryDelay(100)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("취소는 다섯 번째 실패에서 한도에 닿는다")
    void cancelExhaustedAtLimit() {
        assertThat(policy.isCancelExhausted(4)).isFalse();
        assertThat(policy.isCancelExhausted(5)).isTrue();
    }

}