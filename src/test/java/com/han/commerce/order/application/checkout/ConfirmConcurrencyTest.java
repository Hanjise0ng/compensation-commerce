package com.han.commerce.order.application.checkout;

import com.han.commerce.common.domain.Money;
import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.domain.saga.SagaStatus;
import com.han.commerce.order.infrastructure.persistence.OrderJpaRepository;
import com.han.commerce.order.infrastructure.persistence.OrderSagaJpaRepository;
import com.han.commerce.payment.application.port.result.Approved;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryType;
import com.han.commerce.stock.infrastructure.persistence.StockHistoryJpaRepository;
import com.han.commerce.stock.infrastructure.persistence.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private PaymentAttemptService attemptService;

    @Autowired
    private OrderConfirmService confirmService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderSagaJpaRepository sagaRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockHistoryJpaRepository stockHistoryRepository;

    @Test
    @DisplayName("사가와 복구 워커가 같은 주문을 동시에 확정해도 재고는 한 번만 차감된다")
    void confirmOnlyOnce() throws InterruptedException {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        PlaceOrderResult placed = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), null, 0));
        attemptService.start(new ConfirmPaymentCommand(memberId, placed.getOrderNo(), "pay-key-1",
                placed.getPayAmount()));
        Long orderId = orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow().getId();
        Approved approved = new Approved("pay-key-1", Money.of(placed.getPayAmount()), "CARD", clock.now());

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    confirmService.confirm(orderId, approved);
                    succeeded.incrementAndGet();
                } catch (RuntimeException e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
                return null;
            });
        }
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getOnHand()).isEqualTo(8);
        assertThat(stock.getReserved()).isEqualTo(0);
        assertThat(stockHistoryRepository.countByOrderIdAndType(orderId, StockHistoryType.COMMIT)).isEqualTo(1L);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

}