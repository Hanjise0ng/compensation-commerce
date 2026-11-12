package com.han.commerce.coupon.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_coupon")
public class MemberCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long couponId;

    @Enumerated(EnumType.STRING)
    private MemberCouponStatus status;

    private LocalDateTime usedAt;

    @Version
    private Long version;

    protected MemberCoupon() {
    }

    public static MemberCoupon issue(Long memberId, Long couponId) {
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.memberId = memberId;
        memberCoupon.couponId = couponId;
        memberCoupon.status = MemberCouponStatus.AVAILABLE;
        return memberCoupon;
    }

    public void use(Long memberId, LocalDateTime now) {
        if (!this.memberId.equals(memberId)) {
            throw new DomainException(ErrorCode.COUPON_NOT_OWNED);
        }
        if (status != MemberCouponStatus.AVAILABLE) {
            throw new DomainException(ErrorCode.COUPON_ALREADY_USED);
        }
        this.status = MemberCouponStatus.USED;
        this.usedAt = now;
    }

    public void restore() {
        if (status != MemberCouponStatus.USED) {
            throw new DomainException(ErrorCode.COUPON_NOT_USED);
        }
        this.status = MemberCouponStatus.AVAILABLE;
        this.usedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public MemberCouponStatus getStatus() {
        return status;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

}