package com.han.commerce.order.application.checkout;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.checkout.dto.PaymentAttempt;
import com.han.commerce.order.application.recovery.policy.RecoveryPolicy;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.order.domain.saga.OrderSaga;
import com.han.commerce.order.domain.saga.OrderSagaRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentAttemptService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderSagaRepository sagaRepository;
    private final RecoveryPolicy recoveryPolicy;
    private final ClockHolder clockHolder;

    public PaymentAttemptService(OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 OrderSagaRepository sagaRepository,
                                 RecoveryPolicy recoveryPolicy,
                                 ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.sagaRepository = sagaRepository;
        this.recoveryPolicy = recoveryPolicy;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public PaymentAttempt start(ConfirmPaymentCommand command) {
        Order order = orderRepository.findByOrderNo(command.getOrderNo())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, command.getOrderNo()));
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));

        order.startPayment(command.getMemberId(), Money.of(command.getAmount()));

        if (sagaRepository.findByOrderId(order.getId()).isPresent()) {
            throw new DomainException(ErrorCode.PAYMENT_IN_PROGRESS, order.getOrderNo());
        }

        payment.attempt(command.getPaymentKey());

        LocalDateTime now = clockHolder.now();
        sagaRepository.save(OrderSaga.start(order.getId(), command.getPaymentKey(), now,
                now.plus(recoveryPolicy.firstCheckDelay())));

        return new PaymentAttempt(order.getId(), order.getOrderNo(), command.getPaymentKey(), order.getPayAmount());
    }

}