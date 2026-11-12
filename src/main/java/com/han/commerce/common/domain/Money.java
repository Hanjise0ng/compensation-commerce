package com.han.commerce.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Money {

    public static final Money ZERO = new Money(0L);

    @Column(name = "amount", nullable = false)
    private long amount;

    protected Money() {
    }

    private Money(long amount) {
        if (amount < 0) {
            throw new DomainException(ErrorCode.INVALID_MONEY, String.valueOf(amount));
        }
        this.amount = amount;
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(amount, other.amount));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(amount, other.amount));
    }

    public Money times(int quantity) {
        return new Money(Math.multiplyExact(amount, (long) quantity));
    }

    public boolean isGreaterThan(Money other) {
        return amount > other.amount;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return amount == other.amount;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(amount);
    }

    @Override
    public String toString() {
        return amount + "원";
    }

}