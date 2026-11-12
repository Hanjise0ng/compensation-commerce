package com.han.commerce.coupon.infrastructure;

import com.han.commerce.coupon.domain.MemberCoupon;
import com.han.commerce.coupon.domain.MemberCouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberCouponJpaRepository extends MemberCouponRepository, JpaRepository<MemberCoupon, Long> {

    @Override
    Optional<MemberCoupon> findById(Long id);

}