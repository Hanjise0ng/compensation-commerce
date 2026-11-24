package com.han.commerce.order.infrastructure.scheduler;

import com.han.commerce.order.application.recovery.OrderSagaService;
import com.han.commerce.order.application.recovery.PaymentRecoveryService;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "commerce.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentRecoveryScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final OrderSagaService sagaService;
    private final PaymentRecoveryService recoveryService;

    public PaymentRecoveryScheduler(OrderSagaService sagaService, PaymentRecoveryService recoveryService) {
        this.sagaService = sagaService;
        this.recoveryService = recoveryService;
    }

    @Scheduled(fixedDelayString = "${commerce.payment.recovery-interval-ms:10000}")
    public void recoverPayments() {
        for (Long sagaId : sagaService.findDueSagaIds(BATCH_SIZE)) {
            try {
                RecoveryOutcome outcome = recoveryService.recover(sagaId);
                log.info("결제 복구 sagaId={} outcome={}", sagaId, outcome);
            } catch (RuntimeException e) {
                log.warn("결제 복구 실패 sagaId={} reason={}", sagaId, e.getMessage());
            }
        }
    }

}