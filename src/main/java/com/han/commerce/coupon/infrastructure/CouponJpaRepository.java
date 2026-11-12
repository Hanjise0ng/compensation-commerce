package com.han.commerce.coupon.infrastructure;

import com.han.commerce.coupon.domain.Coupon;
import com.han.commerce.coupon.domain.CouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponJpaRepository extends CouponRepository, JpaRepository<Coupon, Long> {

    @Override
    Optional<Coupon> findById(Long id);

}