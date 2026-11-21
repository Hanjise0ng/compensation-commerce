package com.han.commerce.payment.application;

public interface PaymentGateway {

    PaymentResult confirm(PaymentConfirmRequest request);

    PaymentResult inquire(PaymentInquiryRequest request);

}