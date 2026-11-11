package com.han.commerce.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    @DisplayName("금액이 같으면 같은 값이다")
    void equality() {
        assertThat(Money.of(1_000)).isEqualTo(Money.of(1_000));
        assertThat(Money.of(1_000).hashCode()).isEqualTo(Money.of(1_000).hashCode());
    }

    @Test
    @DisplayName("더하기, 빼기, 곱하기는 새 객체를 돌려준다")
    void arithmetic() {
        Money price = Money.of(10_000);

        assertThat(price.plus(Money.of(500))).isEqualTo(Money.of(10_500));
        assertThat(price.minus(Money.of(3_000))).isEqualTo(Money.of(7_000));
        assertThat(price.times(3)).isEqualTo(Money.of(30_000));
        assertThat(price).isEqualTo(Money.of(10_000));
    }

    @Test
    @DisplayName("음수 금액은 만들 수 없다")
    void negativeIsRejected() {
        assertErrorCode(() -> Money.of(-1), ErrorCode.INVALID_MONEY);
        assertErrorCode(() -> Money.of(1_000).minus(Money.of(1_001)), ErrorCode.INVALID_MONEY);
    }

    @Test
    @DisplayName("크기 비교와 0 여부를 판단한다")
    void comparison() {
        assertThat(Money.of(2).isGreaterThan(Money.of(1))).isTrue();
        assertThat(Money.of(1).isGreaterThan(Money.of(1))).isFalse();
        assertThat(Money.ZERO.isZero()).isTrue();
    }

}