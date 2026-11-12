package com.han.commerce.coupon.domain;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 12, 0);
    private static final LocalDateTime FROM = NOW.minusDays(1);
    private static final LocalDateTime UNTIL = NOW.plusDays(30);

    @Test
    @DisplayName("정액 쿠폰은 정해진 금액을 할인한다")
    void fixedDiscount() {
        Coupon coupon = Coupon.fixed("3천원 할인", Money.of(3_000), Money.of(10_000), FROM, UNTIL);

        assertThat(coupon.calculateDiscount(Money.of(20_000), NOW)).isEqualTo(Money.of(3_000));
    }

    @Test
    @DisplayName("정률 쿠폰은 할인 상한을 넘지 않는다")
    void percentDiscountWithCap() {
        Coupon coupon = Coupon.percent("10% 할인", 10, Money.ZERO, Money.of(5_000), FROM, UNTIL);

        assertThat(coupon.calculateDiscount(Money.of(30_000), NOW)).isEqualTo(Money.of(3_000));
        assertThat(coupon.calculateDiscount(Money.of(100_000), NOW)).isEqualTo(Money.of(5_000));
    }

    @Test
    @DisplayName("할인액은 주문 금액을 넘지 않는다")
    void discountNeverExceedsOrderAmount() {
        Coupon coupon = Coupon.fixed("3천원 할인", Money.of(3_000), Money.ZERO, FROM, UNTIL);

        assertThat(coupon.calculateDiscount(Money.of(2_000), NOW)).isEqualTo(Money.of(2_000));
    }

    @Test
    @DisplayName("최소 주문 금액에 못 미치거나 유효기간이 아니면 적용할 수 없다")
    void notApplicable() {
        Coupon coupon = Coupon.fixed("3천원 할인", Money.of(3_000), Money.of(10_000), FROM, UNTIL);

        assertErrorCode(() -> coupon.calculateDiscount(Money.of(9_999), NOW), ErrorCode.COUPON_NOT_APPLICABLE);
        assertErrorCode(() -> coupon.calculateDiscount(Money.of(20_000), UNTIL), ErrorCode.COUPON_NOT_APPLICABLE);
        assertErrorCode(() -> coupon.calculateDiscount(Money.of(20_000), FROM.minusSeconds(1)),
                ErrorCode.COUPON_NOT_APPLICABLE);
    }

    @Test
    @DisplayName("회원 쿠폰은 본인만, 한 번만 쓸 수 있다")
    void useMemberCoupon() {
        MemberCoupon memberCoupon = MemberCoupon.issue(1L, 10L);

        assertErrorCode(() -> memberCoupon.use(2L, NOW), ErrorCode.COUPON_NOT_OWNED);

        memberCoupon.use(1L, NOW);
        assertThat(memberCoupon.getStatus()).isEqualTo(MemberCouponStatus.USED);
        assertThat(memberCoupon.getUsedAt()).isEqualTo(NOW);

        assertErrorCode(() -> memberCoupon.use(1L, NOW), ErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("사용한 쿠폰만 되돌릴 수 있다 (use의 보상)")
    void restoreMemberCoupon() {
        MemberCoupon memberCoupon = MemberCoupon.issue(1L, 10L);

        assertErrorCode(memberCoupon::restore, ErrorCode.COUPON_NOT_USED);

        memberCoupon.use(1L, NOW);
        memberCoupon.restore();

        assertThat(memberCoupon.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(memberCoupon.getUsedAt()).isNull();
    }

}