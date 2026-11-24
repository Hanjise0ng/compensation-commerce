package com.han.commerce.order.application.checkout;

import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.checkout.dto.PaymentAttempt;
import com.han.commerce.order.application.checkout.dto.PaymentConfirmResult;
import com.han.commerce.order.application.compensation.OrderCompensationService;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import com.han.commerce.payment.application.port.result.Approved;
import com.han.commerce.payment.application.port.result.Declined;
import com.han.commerce.payment.application.port.PaymentGateway;
import com.han.commerce.payment.application.port.result.PaymentResult;
import com.han.commerce.payment.application.port.result.Unknown;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentSaga {

    private final PaymentAttemptService attemptService;
    private final ApprovedPaymentFinisher paymentFinisher;
    private final OrderCompensationService compensationService;
    private final PaymentGateway paymentGateway;

    public OrderPaymentSaga(PaymentAttemptService attemptService,
                            ApprovedPaymentFinisher paymentFinisher,
                            OrderCompensationService compensationService,
                            PaymentGateway paymentGateway) {
        this.attemptService = attemptService;
        this.paymentFinisher = paymentFinisher;
        this.compensationService = compensationService;
        this.paymentGateway = paymentGateway;
    }

    public PaymentConfirmResult confirm(ConfirmPaymentCommand command) {
        PaymentAttempt attempt = attemptService.start(command);

        PaymentResult result = paymentGateway.confirm(attempt.toRequest());

        return switch (result) {
            case Approved approved -> toResult(attempt, paymentFinisher.finish(attempt.getOrderId(), approved));
            case Declined declined -> {
                compensationService.compensate(attempt.getOrderId(), declined.getCode(), declined.getMessage());
                yield PaymentConfirmResult.failed(attempt.getOrderNo(), declined.getMessage());
            }
            case Unknown unknown -> PaymentConfirmResult.pending(attempt.getOrderNo(), "결제 결과를 확인하고 있습니다.");
        };
    }

    private PaymentConfirmResult toResult(PaymentAttempt attempt, RecoveryOutcome outcome) {
        return switch (outcome) {
            case CONFIRMED -> PaymentConfirmResult.paid(attempt.getOrderNo());
            case CANCELED -> PaymentConfirmResult.failed(attempt.getOrderNo(),
                    "주문을 완료하지 못해 결제를 취소했습니다.");
            case CANCEL_POSTPONED, CANCEL_GAVE_UP -> PaymentConfirmResult.pending(attempt.getOrderNo(),
                    "주문을 완료하지 못해 결제 취소를 진행하고 있습니다.");
            default -> PaymentConfirmResult.pending(attempt.getOrderNo(), "결제 결과를 확인하고 있습니다.");
        };
    }

}