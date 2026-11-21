package com.han.commerce.order.application;

import com.han.commerce.payment.application.CancelFailed;
import com.han.commerce.payment.application.CancelResult;
import com.han.commerce.payment.application.Canceled;
import com.han.commerce.payment.application.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentCancelCompensator {

    private final PaymentCancelService cancelService;
    private final OrderCompensationService compensationService;
    private final OrderSagaService sagaService;
    private final PaymentGateway paymentGateway;

    public PaymentCancelCompensator(PaymentCancelService cancelService,
                                    OrderCompensationService compensationService,
                                    OrderSagaService sagaService,
                                    PaymentGateway paymentGateway) {
        this.cancelService = cancelService;
        this.compensationService = compensationService;
        this.sagaService = sagaService;
        this.paymentGateway = paymentGateway;
    }

    public RecoveryOutcome run(Long sagaId) {
        Optional<CancelTarget> found = cancelService.loadTarget(sagaId);
        if (found.isEmpty()) {
            return RecoveryOutcome.SKIPPED;
        }
        CancelTarget target = found.get();

        CancelResult result = paymentGateway.cancel(target.toCancelRequest());

        return switch (result) {
            case Canceled canceled -> {
                compensationService.compensateCanceled(target.getOrderId(), canceled.getCanceledAt());
                yield RecoveryOutcome.CANCELED;
            }
            case CancelFailed failed -> {
                sagaService.postpone(sagaId, failed.getReason());
                yield RecoveryOutcome.CANCEL_POSTPONED;
            }
        };
    }

}