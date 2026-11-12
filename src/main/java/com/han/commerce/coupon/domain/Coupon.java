package com.han.commerce.coupon.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private long discountValue;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "min_order_amount", nullable = false))
    private Money minOrderAmount;

    private Long maxDiscountAmount;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    protected Coupon() {
    }

    public static Coupon fixed(String name, Money discount, Money minOrderAmount,
                               LocalDateTime validFrom, LocalDateTime validUntil) {
        return create(name, DiscountType.FIXED, discount.getAmount(), minOrderAmount, null, validFrom, validUntil);
    }

    public static Coupon percent(String name, int percent, Money minOrderAmount, Money maxDiscountAmount,
                                 LocalDateTime validFrom, LocalDateTime validUntil) {
        if (percent > 100) {
            throw new DomainException(ErrorCode.INVALID_COUPON, "할인율 " + percent);
        }
        Long max = maxDiscountAmount == null ? null : maxDiscountAmount.getAmount();
        return create(name, DiscountType.PERCENT, percent, minOrderAmount, max, validFrom, validUntil);
    }

    private static Coupon create(String name, DiscountType type, long value, Money minOrderAmount,
                                 Long maxDiscountAmount, LocalDateTime validFrom, LocalDateTime validUntil) {
        if (value <= 0 || !validFrom.isBefore(validUntil)) {
            throw new DomainException(ErrorCode.INVALID_COUPON, name);
        }
        Coupon coupon = new Coupon();
        coupon.name = name;
        coupon.discountType = type;
        coupon.discountValue = value;
        coupon.minOrderAmount = minOrderAmount;
        coupon.maxDiscountAmount = maxDiscountAmount;
        coupon.validFrom = validFrom;
        coupon.validUntil = validUntil;
        return coupon;
    }

    public Money calculateDiscount(Money orderAmount, LocalDateTime now) {
        if (now.isBefore(validFrom) || !now.isBefore(validUntil)) {
            throw new DomainException(ErrorCode.COUPON_NOT_APPLICABLE, "유효기간이 아닙니다");
        }
        if (minOrderAmount.isGreaterThan(orderAmount)) {
            throw new DomainException(ErrorCode.COUPON_NOT_APPLICABLE, "최소 주문 금액 " + minOrderAmount);
        }
        long discount = switch (discountType) {
            case FIXED -> discountValue;
            case PERCENT -> orderAmount.getAmount() * discountValue / 100;
        };
        if (maxDiscountAmount != null) {
            discount = Math.min(discount, maxDiscountAmount);
        }
        return Money.of(Math.min(discount, orderAmount.getAmount()));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}