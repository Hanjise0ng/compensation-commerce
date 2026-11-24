package com.han.commerce.order.application.checkout;

import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.order.application.compensation.PaymentCancelCompensator;
import com.han.commerce.order.application.compensation.PaymentCancelService;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import com.han.commerce.payment.application.port.result.Approved;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApprovedPaymentFinisher {

    private final OrderConfirmService confirmService;
    private final PaymentCancelService cancelService;
    private final PaymentCancelCompensator cancelCompensator;

    public ApprovedPaymentFinisher(OrderConfirmService confirmService,
                                   PaymentCancelService cancelService,
                                   PaymentCancelCompensator cancelCompensator) {
        this.confirmService = confirmService;
        this.cancelService = cancelService;
        this.cancelCompensator = cancelCompensator;
    }

    public RecoveryOutcome finish(Long orderId, Approved approved) {
        try {
            confirmService.confirm(orderId, approved);
            return RecoveryOutcome.CONFIRMED;
        } catch (DomainException e) {
            Optional<Long> sagaId = cancelService.request(orderId, approved.getApprovedAmount(),
                    "주문 확정 실패: " + e.getErrorCode().name());
            if (sagaId.isEmpty()) {
                return RecoveryOutcome.SKIPPED;
            }
            return cancelCompensator.run(sagaId.get());
        }
    }

}