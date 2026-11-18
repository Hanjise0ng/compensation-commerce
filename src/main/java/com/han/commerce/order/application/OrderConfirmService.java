package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.application.Approved;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderConfirmService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStockCommitter stockCommitter;
    private final ClockHolder clockHolder;

    public OrderConfirmService(OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               OrderStockCommitter stockCommitter,
                               ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.stockCommitter = stockCommitter;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public void confirm(Long orderId, Approved approved) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.approve(approved.getMethod(), approved.getApprovedAt());
        order.completePayment(approved.getApprovedAmount(), clockHolder.now());
        stockCommitter.commit(order);
    }

}