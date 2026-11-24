package com.han.commerce.payment.application.port;

import com.han.commerce.payment.application.port.request.PaymentCancelRequest;
import com.han.commerce.payment.application.port.request.PaymentConfirmRequest;
import com.han.commerce.payment.application.port.request.PaymentInquiryRequest;
import com.han.commerce.payment.application.port.result.CancelResult;
import com.han.commerce.payment.application.port.result.PaymentResult;

public interface PaymentGateway {

    PaymentResult confirm(PaymentConfirmRequest request);

    PaymentResult inquire(PaymentInquiryRequest request);

    CancelResult cancel(PaymentCancelRequest request);

}