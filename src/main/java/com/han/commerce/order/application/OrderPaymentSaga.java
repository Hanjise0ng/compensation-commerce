package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.payment.application.Approved;
import com.han.commerce.payment.application.Declined;
import com.han.commerce.payment.application.PaymentGateway;
import com.han.commerce.payment.application.PaymentResult;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentSaga {

    private final PaymentAttemptService attemptService;
    private final OrderConfirmService confirmService;
    private final PaymentGateway paymentGateway;

    public OrderPaymentSaga(PaymentAttemptService attemptService,
                            OrderConfirmService confirmService,
                            PaymentGateway paymentGateway) {
        this.attemptService = attemptService;
        this.confirmService = confirmService;
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
            case Declined declined -> throw new DomainException(ErrorCode.PAYMENT_DECLINED, declined.getMessage());
        };
    }

}