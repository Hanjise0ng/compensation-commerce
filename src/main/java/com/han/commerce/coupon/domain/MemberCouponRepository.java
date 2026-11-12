package com.han.commerce.coupon.domain;

import java.util.Optional;

public interface MemberCouponRepository {

    Optional<MemberCoupon> findById(Long id);

}