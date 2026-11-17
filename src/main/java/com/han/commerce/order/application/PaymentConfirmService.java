package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.application.*;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentConfirmService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final OrderStockCommitter stockCommitter;
    private final ClockHolder clockHolder;

    public PaymentConfirmService(OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 PaymentGateway paymentGateway,
                                 OrderStockCommitter stockCommitter,
                                 ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.stockCommitter = stockCommitter;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public PaymentConfirmResult confirm(ConfirmPaymentCommand command) {
        Order order = orderRepository.findByOrderNo(command.getOrderNo())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, command.getOrderNo()));
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));

        order.startPayment(command.getMemberId(), Money.of(command.getAmount()));
        payment.attempt(command.getPaymentKey());

        PaymentResult result = paymentGateway.confirm(
                new PaymentConfirmRequest(order.getOrderNo(), command.getPaymentKey(), order.getPayAmount()));

        return switch (result) {
            case Approved approved -> {
                payment.approve(approved.getMethod(), approved.getApprovedAt());
                order.completePayment(approved.getApprovedAmount(), clockHolder.now());
                stockCommitter.commit(order);
                yield PaymentConfirmResult.paid(order.getOrderNo());
            }
            case Declined declined -> throw new DomainException(ErrorCode.PAYMENT_DECLINED, declined.getMessage());
        };
    }

}