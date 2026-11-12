package com.han.commerce.point.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "point_wallet")
public class PointWallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "balance", nullable = false))
    private Money balance;

    @Version
    private Long version;

    protected PointWallet() {
    }

    public static PointWallet create(Long memberId) {
        PointWallet wallet = new PointWallet();
        wallet.memberId = memberId;
        wallet.balance = Money.ZERO;
        return wallet;
    }

    public PointHistory charge(Money amount) {
        requirePositive(amount);
        this.balance = balance.plus(amount);
        return PointHistory.of(memberId, null, PointHistoryType.CHARGE, amount, balance);
    }

    public PointHistory use(Money amount, Long orderId) {
        requirePositive(amount);
        if (amount.isGreaterThan(balance)) {
            throw new DomainException(ErrorCode.INSUFFICIENT_POINT, "잔액 " + balance + ", 요청 " + amount);
        }
        this.balance = balance.minus(amount);
        return PointHistory.of(memberId, orderId, PointHistoryType.USE, amount, balance);
    }

    public PointHistory restore(Money amount, Long orderId) {
        requirePositive(amount);
        this.balance = balance.plus(amount);
        return PointHistory.of(memberId, orderId, PointHistoryType.RESTORE, amount, balance);
    }

    private void requirePositive(Money amount) {
        if (amount.isZero()) {
            throw new DomainException(ErrorCode.INVALID_MONEY, "포인트 금액은 0보다 커야 합니다");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Money getBalance() {
        return balance;
    }

}