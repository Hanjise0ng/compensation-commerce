package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAttemptService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentAttemptService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentAttempt start(ConfirmPaymentCommand command) {
        Order order = orderRepository.findByOrderNo(command.getOrderNo())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, command.getOrderNo()));
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));

        order.startPayment(command.getMemberId(), Money.of(command.getAmount()));
        payment.attempt(command.getPaymentKey());

        return new PaymentAttempt(order.getId(), order.getOrderNo(), command.getPaymentKey(), order.getPayAmount());
    }

}