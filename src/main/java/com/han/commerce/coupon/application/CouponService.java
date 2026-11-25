package com.han.commerce.coupon.application;

import com.han.commerce.common.domain.Money;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.coupon.domain.Coupon;
import com.han.commerce.coupon.domain.CouponRepository;
import com.han.commerce.coupon.domain.MemberCoupon;
import com.han.commerce.coupon.domain.MemberCouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    public CouponService(CouponRepository couponRepository, MemberCouponRepository memberCouponRepository) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
    }

    public Money use(Long memberCouponId, Long memberId, Money orderAmount, LocalDateTime now) {
        MemberCoupon memberCoupon = findMemberCoupon(memberCouponId);
        Coupon coupon = couponRepository.findById(memberCoupon.getCouponId())
                .orElseThrow(() -> new DomainException(ErrorCode.COUPON_NOT_FOUND));

        Money discount = coupon.calculateDiscount(orderAmount, now);
        memberCoupon.use(memberId, now);
        return discount;
    }

    public void restore(Long memberCouponId) {
        findMemberCoupon(memberCouponId).restore();
    }

    private MemberCoupon findMemberCoupon(Long memberCouponId) {
        return memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new DomainException(ErrorCode.COUPON_NOT_FOUND));
    }

}