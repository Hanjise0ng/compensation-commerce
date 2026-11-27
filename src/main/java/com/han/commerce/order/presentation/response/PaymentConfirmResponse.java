package com.han.commerce.order.presentation.response;

import com.han.commerce.order.application.checkout.dto.PaymentConfirmResult;

public class PaymentConfirmResponse {

    private final String orderNo;
    private final String status;
    private final String message;

    private PaymentConfirmResponse(String orderNo, String status, String message) {
        this.orderNo = orderNo;
        this.status = status;
        this.message = message;
    }

    public static PaymentConfirmResponse from(PaymentConfirmResult result) {
        return new PaymentConfirmResponse(result.getOrderNo(), result.getStatus().name(), result.getMessage());
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}