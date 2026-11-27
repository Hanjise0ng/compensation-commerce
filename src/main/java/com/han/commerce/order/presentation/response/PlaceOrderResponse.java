package com.han.commerce.order.presentation.response;

import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import java.time.LocalDateTime;

public class PlaceOrderResponse {

    private final String orderNo;
    private final String status;
    private final long payAmount;
    private final LocalDateTime expiresAt;

    private PlaceOrderResponse(String orderNo, String status, long payAmount, LocalDateTime expiresAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.payAmount = payAmount;
        this.expiresAt = expiresAt;
    }

    public static PlaceOrderResponse from(PlaceOrderResult result) {
        return new PlaceOrderResponse(result.getOrderNo(), result.getStatus().name(),
                result.getPayAmount(), result.getExpiresAt());
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