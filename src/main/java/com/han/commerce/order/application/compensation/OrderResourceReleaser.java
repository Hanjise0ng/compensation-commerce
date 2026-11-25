package com.han.commerce.order.application.compensation;

import com.han.commerce.coupon.application.CouponService;
import com.han.commerce.order.domain.Order;
import com.han.commerce.point.application.PointService;
import com.han.commerce.stock.application.StockService;
import org.springframework.stereotype.Component;

@Component
public class OrderResourceReleaser {

    private final StockService stockService;
    private final CouponService couponService;
    private final PointService pointService;

    public OrderResourceReleaser(StockService stockService,
                                 CouponService couponService,
                                 PointService pointService) {
        this.stockService = stockService;
        this.couponService = couponService;
        this.pointService = pointService;
    }

    public void release(Order order) {
        stockService.release(order.getId(), order.quantityByProductId());
        if (order.usesCoupon()) {
            couponService.restore(order.getMemberCouponId());
        }
        if (order.usesPoint()) {
            pointService.restore(order.getMemberId(), order.getPointAmount(), order.getId());
        }
    }

}