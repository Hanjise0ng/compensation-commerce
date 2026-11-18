package com.han.commerce.order.application;

import com.han.commerce.payment.application.Approved;
import com.han.commerce.payment.application.Declined;
import com.han.commerce.payment.application.PaymentGateway;
import com.han.commerce.payment.application.PaymentResult;
import com.han.commerce.payment.application.Unknown;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentSaga {

    private final PaymentAttemptService attemptService;
    private final OrderConfirmService confirmService;
    private final OrderCompensationService compensationService;
    private final PaymentGateway paymentGateway;

    public OrderPaymentSaga(PaymentAttemptService attemptService,
                            OrderConfirmService confirmService,
                            OrderCompensationService compensationService,
                            PaymentGateway paymentGateway) {
        this.attemptService = attemptService;
        this.confirmService = confirmService;
        this.compensationService = compensationService;
        this.paymentGateway = paymentGateway;
    }

    public PaymentConfirmResult confirm(ConfirmPaymentCommand command) {
        PaymentAttempt attempt = attemptService.start(command);

        PaymentResult result = paymentGateway.confirm(attempt.toRequest());

        return switch (result) {
            case Approved approved -> {
                confirmService.confirm(attempt.getOrderId(), approved);
                yield PaymentConfirmResult.paid(attempt.getOrderNo());
            }
            case Declined declined -> {
                compensationService.compensate(attempt.getOrderId(), declined.getCode(), declined.getMessage());
                yield PaymentConfirmResult.failed(attempt.getOrderNo(), declined.getMessage());
            }
            case Unknown unknown -> PaymentConfirmResult.pending(attempt.getOrderNo(), "결제 결과를 확인하고 있습니다.");
        };
    }

}