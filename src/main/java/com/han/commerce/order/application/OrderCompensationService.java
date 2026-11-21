package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.order.domain.OrderSagaRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCompensationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderSagaRepository sagaRepository;
    private final OrderResourceReleaser resourceReleaser;
    private final ClockHolder clockHolder;

    public OrderCompensationService(OrderRepository orderRepository,
                                    PaymentRepository paymentRepository,
                                    OrderSagaRepository sagaRepository,
                                    OrderResourceReleaser resourceReleaser,
                                    ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.sagaRepository = sagaRepository;
        this.resourceReleaser = resourceReleaser;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public void compensate(Long orderId, String failureCode, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        order.fail(reason);

        resourceReleaser.release(order);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.fail(failureCode, reason);

        sagaRepository.findByOrderId(orderId).ifPresent(saga -> saga.compensated(clockHolder.now()));
    }

}