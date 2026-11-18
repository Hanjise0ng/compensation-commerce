package com.han.commerce.order.application;

import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.MemberCouponStatus;
import com.han.commerce.coupon.infrastructure.MemberCouponJpaRepository;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.point.infrastructure.PointHistoryJpaRepository;
import com.han.commerce.point.infrastructure.PointWalletJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryType;
import com.han.commerce.stock.infrastructure.StockHistoryJpaRepository;
import com.han.commerce.stock.infrastructure.StockJpaRepository;
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

class CompensationConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderCompensationService compensationService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockHistoryJpaRepository stockHistoryRepository;

    @Autowired
    private MemberCouponJpaRepository memberCouponRepository;

    @Autowired
    private PointWalletJpaRepository pointWalletRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryRepository;

    @Test
    @DisplayName("같은 주문을 두 스레드가 동시에 보상해도 자원은 한 번만 돌아온다")
    void compensateOnlyOnce() throws InterruptedException {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        Long memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);
        PlaceOrderResult placed = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000));
        Long orderId = orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow().getId();

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
                    compensationService.compensate(orderId, "REJECT_CARD_COMPANY", "한도 초과");
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

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.FAILED);
        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(0);
        assertThat(stockHistoryRepository.countByOrderIdAndType(orderId, StockHistoryType.RELEASE)).isEqualTo(1L);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
        assertThat(pointHistoryRepository.count()).isEqualTo(3L);
    }

}