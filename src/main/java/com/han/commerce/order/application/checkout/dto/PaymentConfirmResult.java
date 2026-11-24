package com.han.commerce.order.application.checkout.dto;

public class PaymentConfirmResult {

    private final String orderNo;
    private final PaymentConfirmStatus status;
    private final String message;

    private PaymentConfirmResult(String orderNo, PaymentConfirmStatus status, String message) {
        this.orderNo = orderNo;
        this.status = status;
        this.message = message;
    }

    public static PaymentConfirmResult paid(String orderNo) {
        return new PaymentConfirmResult(orderNo, PaymentConfirmStatus.PAID, null);
    }

    public static PaymentConfirmResult failed(String orderNo, String message) {
        return new PaymentConfirmResult(orderNo, PaymentConfirmStatus.FAILED, message);
    }

    public static PaymentConfirmResult pending(String orderNo, String message) {
        return new PaymentConfirmResult(orderNo, PaymentConfirmStatus.PENDING, message);
    }

    public String getOrderNo() {
        return orderNo;
    }

    public PaymentConfirmStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}