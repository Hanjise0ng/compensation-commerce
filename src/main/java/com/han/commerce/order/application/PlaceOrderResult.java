package com.han.commerce.order.application;

import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderStatus;

import java.time.LocalDateTime;

public class PlaceOrderResult {

    private final String orderNo;
    private final OrderStatus status;
    private final long payAmount;
    private final LocalDateTime expiresAt;

    private PlaceOrderResult(String orderNo, OrderStatus status, long payAmount, LocalDateTime expiresAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.payAmount = payAmount;
        this.expiresAt = expiresAt;
    }

    public static PlaceOrderResult from(Order order) {
        return new PlaceOrderResult(order.getOrderNo(), order.getStatus(),
                order.getPayAmount().getAmount(), order.getExpiresAt());
    }

    public String getOrderNo() {
        return orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getPayAmount() {
        return payAmount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

}