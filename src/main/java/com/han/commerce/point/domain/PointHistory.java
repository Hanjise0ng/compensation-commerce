package com.han.commerce.point.domain;

import com.han.commerce.common.domain.BaseCreatedEntity;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_history")
public class PointHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private PointHistoryType type;

    @Embedded
    private Money amount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "balance_after", nullable = false))
    private Money balanceAfter;

    protected PointHistory() {
    }

    static PointHistory of(Long memberId, Long orderId, PointHistoryType type, Money amount, Money balanceAfter) {
        PointHistory history = new PointHistory();
        history.memberId = memberId;
        history.orderId = orderId;
        history.type = type;
        history.amount = amount;
        history.balanceAfter = balanceAfter;
        return history;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public PointHistoryType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

}