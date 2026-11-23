package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.domain.OrderSagaRepository;
import com.han.commerce.payment.domain.Refund;
import com.han.commerce.payment.domain.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentCancelService {

    private final OrderSagaRepository sagaRepository;
    private final RefundRepository refundRepository;
    private final ClockHolder clockHolder;

    public PaymentCancelService(OrderSagaRepository sagaRepository,
                                RefundRepository refundRepository,
                                ClockHolder clockHolder) {
        this.sagaRepository = sagaRepository;
        this.refundRepository = refundRepository;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public Optional<Long> request(Long orderId, Money approvedAmount, String reason) {
        Optional<OrderSaga> found = sagaRepository.findByOrderId(orderId);
        if (found.isEmpty() || !found.get().isInProgress()) {
            return Optional.empty();
        }
        OrderSaga saga = found.get();
        LocalDateTime now = clockHolder.now();

        saga.startCompensation(reason, now);
        refundRepository.save(Refund.request(orderId, saga.getPaymentKey(), approvedAmount, reason, now));

        return Optional.of(saga.getId());
    }

    @Transactional(readOnly = true)
    public Optional<CancelTarget> loadTarget(Long sagaId) {
        OrderSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new DomainException(ErrorCode.SAGA_NOT_FOUND));
        if (!saga.isCompensating()) {
            return Optional.empty();
        }
        Refund refund = refundRepository.findByOrderId(saga.getOrderId())
                .orElseThrow(() -> new DomainException(ErrorCode.REFUND_NOT_FOUND));
        return Optional.of(new CancelTarget(saga.getId(), saga.getOrderId(), refund.getPaymentKey(),
                refund.getAmount(), refund.getReason()));
    }

}