package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderResourceReleaser resourceReleaser;
    private final ClockHolder clockHolder;

    public OrderExpirationService(OrderRepository orderRepository,
                                  PaymentRepository paymentRepository,
                                  OrderResourceReleaser resourceReleaser,
                                  ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.resourceReleaser = resourceReleaser;
        this.clockHolder = clockHolder;
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds(int limit) {
        return orderRepository.findExpiredOrderIds(clockHolder.now(), limit);
    }

    @Transactional
    public void expire(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        order.expire(clockHolder.now());

        resourceReleaser.release(order);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.fail("ORDER_EXPIRED", "결제 기한 만료");
    }

}