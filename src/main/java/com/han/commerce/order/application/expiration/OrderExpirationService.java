package com.han.commerce.order.application.expiration;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.order.application.compensation.OrderResourceReleaser;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.application.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final OrderResourceReleaser resourceReleaser;
    private final ClockHolder clockHolder;

    public OrderExpirationService(OrderRepository orderRepository,
                                  PaymentService paymentService,
                                  OrderResourceReleaser resourceReleaser,
                                  ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
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
        paymentService.fail(orderId, "ORDER_EXPIRED", "결제 기한 만료");
    }

}