package com.han.commerce.order.application.compensation;

import com.han.commerce.order.application.compensation.dto.CancelTarget;
import com.han.commerce.order.application.recovery.OrderSagaService;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import com.han.commerce.payment.application.port.result.CancelFailed;
import com.han.commerce.payment.application.port.result.CancelResult;
import com.han.commerce.payment.application.port.result.Canceled;
import com.han.commerce.payment.application.port.PaymentGateway;
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
            case CancelFailed failed -> sagaService.recordCancelFailure(sagaId, failed.getReason());
        };
    }

}