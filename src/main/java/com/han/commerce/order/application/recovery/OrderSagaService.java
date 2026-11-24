package com.han.commerce.order.application.recovery;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import com.han.commerce.order.application.recovery.policy.RecoveryPolicy;
import com.han.commerce.order.application.recovery.dto.RecoveryTarget;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.order.domain.saga.OrderSaga;
import com.han.commerce.order.domain.saga.OrderSagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderSagaService {

    private final OrderSagaRepository sagaRepository;
    private final OrderRepository orderRepository;
    private final RecoveryPolicy recoveryPolicy;
    private final ClockHolder clockHolder;

    public OrderSagaService(OrderSagaRepository sagaRepository,
                            OrderRepository orderRepository,
                            RecoveryPolicy recoveryPolicy,
                            ClockHolder clockHolder) {
        this.sagaRepository = sagaRepository;
        this.orderRepository = orderRepository;
        this.recoveryPolicy = recoveryPolicy;
        this.clockHolder = clockHolder;
    }

    @Transactional(readOnly = true)
    public List<Long> findDueSagaIds(int limit) {
        return sagaRepository.findDueIds(clockHolder.now(), limit);
    }

    @Transactional(readOnly = true)
    public Optional<RecoveryTarget> loadTarget(Long sagaId) {
        OrderSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new DomainException(ErrorCode.SAGA_NOT_FOUND));
        if (!saga.isInProgress()) {
            return Optional.empty();
        }
        Order order = orderRepository.findById(saga.getOrderId())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        return Optional.of(new RecoveryTarget(saga.getId(), order.getId(), order.getOrderNo(),
                saga.getPaymentKey(), order.getPayAmount()));
    }

    @Transactional
    public RecoveryOutcome recordCancelFailure(Long sagaId, String reason) {
        OrderSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new DomainException(ErrorCode.SAGA_NOT_FOUND));
        LocalDateTime now = clockHolder.now();
        int attempt = saga.getAttemptCount() + 1;

        if (recoveryPolicy.isCancelExhausted(attempt)) {
            saga.giveUp(reason, now);
            return RecoveryOutcome.CANCEL_GAVE_UP;
        }
        saga.postpone(reason, now.plus(recoveryPolicy.cancelRetryDelay(attempt)));
        return RecoveryOutcome.CANCEL_POSTPONED;
    }

    @Transactional
    public void postpone(Long sagaId, String reason) {
        OrderSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new DomainException(ErrorCode.SAGA_NOT_FOUND));
        saga.postpone(reason, clockHolder.now().plus(recoveryPolicy.retryDelay(saga.getAttemptCount() + 1)));
    }

}