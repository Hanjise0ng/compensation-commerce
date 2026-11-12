package com.han.commerce.order.application;

import com.han.commerce.order.domain.Order;
import java.time.LocalDateTime;

public class PlaceOrderResult {

    private final String orderNo;
    private final String status;
    private final long payAmount;
    private final LocalDateTime expiresAt;

    private PlaceOrderResult(String orderNo, String status, long payAmount, LocalDateTime expiresAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.payAmount = payAmount;
        this.expiresAt = expiresAt;
    }

    public static PlaceOrderResult from(Order order) {
        return new PlaceOrderResult(order.getOrderNo(), order.getStatus().name(),
                order.getPayAmount().getAmount(), order.getExpiresAt());
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getStatus() {
        return status;
    }

    public long getPayAmount() {
        return payAmount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

}