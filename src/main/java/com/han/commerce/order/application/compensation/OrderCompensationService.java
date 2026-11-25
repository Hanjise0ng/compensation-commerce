package com.han.commerce.order.application.compensation;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.order.domain.saga.OrderSagaRepository;
import com.han.commerce.payment.application.PaymentService;
import com.han.commerce.payment.application.dto.RefundInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderCompensationService {

    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private final PaymentService paymentService;
    private final OrderResourceReleaser resourceReleaser;
    private final ClockHolder clockHolder;

    public OrderCompensationService(OrderRepository orderRepository,
                                    OrderSagaRepository sagaRepository,
                                    PaymentService paymentService,
                                    OrderResourceReleaser resourceReleaser,
                                    ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.sagaRepository = sagaRepository;
        this.paymentService = paymentService;
        this.resourceReleaser = resourceReleaser;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public void compensate(Long orderId, String failureCode, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        order.fail(reason);
        resourceReleaser.release(order);
        paymentService.fail(orderId, failureCode, reason);
        sagaRepository.findByOrderId(orderId).ifPresent(saga -> saga.compensated(clockHolder.now()));
    }

    @Transactional
    public void compensateCanceled(Long orderId, LocalDateTime canceledAt) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        RefundInfo refund = paymentService.getRefund(orderId);
        order.fail(refund.getReason());
        resourceReleaser.release(order);
        paymentService.completeCancel(orderId, canceledAt);
        sagaRepository.findByOrderId(orderId).ifPresent(saga -> saga.compensated(clockHolder.now()));
    }

}