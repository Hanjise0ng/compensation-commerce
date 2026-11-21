package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.payment.application.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentRecoveryService {

    private final OrderSagaService sagaService;
    private final ApprovedPaymentFinisher paymentFinisher;
    private final OrderCompensationService compensationService;
    private final PaymentCancelCompensator cancelCompensator;
    private final PaymentGateway paymentGateway;

    public PaymentRecoveryService(OrderSagaService sagaService,
                                  ApprovedPaymentFinisher paymentFinisher,
                                  OrderCompensationService compensationService,
                                  PaymentCancelCompensator cancelCompensator,
                                  PaymentGateway paymentGateway) {
        this.sagaService = sagaService;
        this.paymentFinisher = paymentFinisher;
        this.compensationService = compensationService;
        this.cancelCompensator = cancelCompensator;
        this.paymentGateway = paymentGateway;
    }

    public RecoveryOutcome recover(Long sagaId) {
        Optional<RecoveryTarget> found = sagaService.loadTarget(sagaId);
        if (found.isEmpty()) {
            return cancelCompensator.run(sagaId);
        }
        RecoveryTarget target = found.get();

        PaymentResult result = paymentGateway.inquire(target.toInquiryRequest());

        try {
            return switch (result) {
                case Approved approved -> paymentFinisher.finish(target.getOrderId(), approved);
                case Declined declined -> {
                    compensationService.compensate(target.getOrderId(), declined.getCode(), declined.getMessage());
                    yield RecoveryOutcome.COMPENSATED;
                }
                case Unknown unknown -> {
                    sagaService.postpone(sagaId, unknown.getReason());
                    yield RecoveryOutcome.POSTPONED;
                }
            };
        } catch (OptimisticLockingFailureException | DomainException e) {
            if (sagaService.loadTarget(sagaId).isEmpty()) {
                return RecoveryOutcome.SKIPPED;
            }
            throw e;
        }
    }

}