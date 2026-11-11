package com.han.commerce.point.domain;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class PointWalletTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;

    private PointWallet walletOf(long balance) {
        PointWallet wallet = PointWallet.create(MEMBER_ID);
        wallet.charge(Money.of(balance));
        return wallet;
    }

    @Test
    @DisplayName("포인트를 쓰면 잔액이 줄고 USE 이력이 생긴다")
    void use() {
        PointWallet wallet = walletOf(5_000);

        PointHistory history = wallet.use(Money.of(2_000), ORDER_ID);

        assertThat(wallet.getBalance()).isEqualTo(Money.of(3_000));
        assertThat(history.getType()).isEqualTo(PointHistoryType.USE);
        assertThat(history.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(history.getBalanceAfter()).isEqualTo(Money.of(3_000));
    }

    @Test
    @DisplayName("잔액보다 많이 쓸 수 없다")
    void insufficient() {
        PointWallet wallet = walletOf(5_000);

        assertErrorCode(() -> wallet.use(Money.of(5_001), ORDER_ID), ErrorCode.INSUFFICIENT_POINT);
        assertThat(wallet.getBalance()).isEqualTo(Money.of(5_000));
    }

    @Test
    @DisplayName("복원하면 잔액이 돌아오고 RESTORE 이력이 생긴다 (use의 보상)")
    void restore() {
        PointWallet wallet = walletOf(5_000);
        wallet.use(Money.of(2_000), ORDER_ID);

        PointHistory history = wallet.restore(Money.of(2_000), ORDER_ID);

        assertThat(wallet.getBalance()).isEqualTo(Money.of(5_000));
        assertThat(history.getType()).isEqualTo(PointHistoryType.RESTORE);
    }

    @Test
    @DisplayName("0원은 쓰거나 복원할 수 없다")
    void zeroIsRejected() {
        PointWallet wallet = walletOf(5_000);

        assertErrorCode(() -> wallet.use(Money.ZERO, ORDER_ID), ErrorCode.INVALID_MONEY);
    }

}