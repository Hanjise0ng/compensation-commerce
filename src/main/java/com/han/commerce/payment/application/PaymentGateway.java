package com.han.commerce.payment.application;

public interface PaymentGateway {

    PaymentResult confirm(PaymentConfirmRequest request);

}