package com.han.commerce.order.application.checkout;

import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.application.query.OrderQueryService;
import com.han.commerce.order.application.query.dto.OrderDetail;
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

@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.datasource.hikari.connection-timeout=1000"
})
class ConnectionPoolTest extends IntegrationTestSupport {

    private static final int REQUESTS = 20;

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("PG가 2초 지연돼도 동시 결제 20건이 커넥션 5개로 모두 성공한다")
    void noPoolExhaustion() throws InterruptedException {
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
        OrderDetail detail = orderQueryService.getOrder(memberId, orders.get(0).getOrderNo());
        assertThat(detail.getOrderNo()).isEqualTo(orders.get(0).getOrderNo());

        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(succeeded.get()).isEqualTo(20);
        assertThat(failed.get()).isEqualTo(0);
    }

}