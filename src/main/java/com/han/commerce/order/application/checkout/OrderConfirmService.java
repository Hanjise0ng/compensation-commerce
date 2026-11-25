package com.han.commerce.order.application.checkout;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.order.domain.saga.OrderSagaRepository;
import com.han.commerce.payment.application.PaymentService;
import com.han.commerce.payment.application.port.result.Approved;
import com.han.commerce.stock.application.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderConfirmService {

    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final ClockHolder clockHolder;

    public OrderConfirmService(OrderRepository orderRepository,
                               OrderSagaRepository sagaRepository,
                               PaymentService paymentService,
                               StockService stockService,
                               ClockHolder clockHolder) {
        this.orderRepository = orderRepository;
        this.sagaRepository = sagaRepository;
        this.paymentService = paymentService;
        this.stockService = stockService;
        this.clockHolder = clockHolder;
    }

    @Transactional
    public void confirm(Long orderId, Approved approved) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        LocalDateTime now = clockHolder.now();

        paymentService.approve(orderId, approved.getMethod(), approved.getApprovedAt());
        order.completePayment(approved.getApprovedAmount(), now);
        stockService.commit(orderId, order.quantityByProductId());
        sagaRepository.findByOrderId(orderId).ifPresent(saga -> saga.complete(now));
    }

}