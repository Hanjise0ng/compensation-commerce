package com.han.commerce.coupon.domain;

import java.util.Optional;

public interface CouponRepository {

    Optional<Coupon> findById(Long id);

}