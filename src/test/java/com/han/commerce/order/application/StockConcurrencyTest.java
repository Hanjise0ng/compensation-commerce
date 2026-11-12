package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.infrastructure.StockHistoryJpaRepository;
import com.han.commerce.stock.infrastructure.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StockConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockHistoryJpaRepository stockHistoryRepository;

    @Test
    @DisplayName("재고 10개인 상품에 30명이 동시에 주문하면 정확히 10건만 성공한다")
    void concurrentOrders() throws InterruptedException {
        int customers = 30;
        Product product = fixtures.product("한정판 키보드", 10_000, 10);
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < customers; i++) {
            memberIds.add(fixtures.member());
        }

        ExecutorService pool = Executors.newFixedThreadPool(customers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(customers);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        for (Long memberId : memberIds) {
            pool.submit(() -> {
                try {
                    start.await();
                    placementFacade.place(new PlaceOrderCommand(memberId, "key-" + memberId,
                            List.of(new PlaceOrderCommand.Item(product.getId(), 1)), null, 0));
                    succeeded.incrementAndGet();
                } catch (DomainException e) {
                    if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK) {
                        outOfStock.incrementAndGet();
                    }
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
        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(outOfStock.get()).isEqualTo(20);

        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(10);
        assertThat(stockHistoryRepository.count()).isEqualTo(11L);
    }

}