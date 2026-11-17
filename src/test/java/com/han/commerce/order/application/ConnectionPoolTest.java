package com.han.commerce.order.application;

import com.han.commerce.product.domain.Product;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.datasource.hikari.connection-timeout=1000"
})
class ConnectionPoolTest extends IntegrationTestSupport {

    private static final int REQUESTS = 20;

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private PaymentConfirmService paymentConfirm;

    @Autowired
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("[문제 재현] PG가 2초 지연되면 동시 결제 20건 중 15건이 커넥션을 얻지 못해 실패한다")
    void poolExhaustion() throws InterruptedException {
        Long memberId = fixtures.member();
        List<PlaceOrderResult> orders = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            Product product = fixtures.product("상품 " + i, 10_000, 10);
            orders.add(placementFacade.place(new PlaceOrderCommand(memberId, "key-" + i,
                    List.of(new PlaceOrderCommand.Item(product.getId(), 1)), null, 0)));
        }
        paymentGateway.delay(2_000);

        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(REQUESTS);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (PlaceOrderResult order : orders) {
            pool.submit(() -> {
                try {
                    start.await();
                    paymentConfirm.confirm(new ConfirmPaymentCommand(
                            memberId, order.getOrderNo(), "pay-" + order.getOrderNo(), order.getPayAmount()));
                    succeeded.incrementAndGet();
                } catch (RuntimeException e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
                return null;
            });
        }
        start.countDown();

        Thread.sleep(300);
        String anyOrderNo = orders.get(0).getOrderNo();
        assertThatThrownBy(() -> orderQueryService.getOrder(memberId, anyOrderNo))
                .isInstanceOf(RuntimeException.class);

        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(succeeded.get()).isEqualTo(5);
        assertThat(failed.get()).isEqualTo(15);
    }

}