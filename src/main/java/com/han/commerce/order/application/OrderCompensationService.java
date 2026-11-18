package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCompensationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderResourceReleaser resourceReleaser;

    public OrderCompensationService(OrderRepository orderRepository,
                                    PaymentRepository paymentRepository,
                                    OrderResourceReleaser resourceReleaser) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.resourceReleaser = resourceReleaser;
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
    }

}