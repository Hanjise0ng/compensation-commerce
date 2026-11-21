package com.han.commerce.order.application;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.order.infrastructure.OrderSagaJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentInProgressTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderSagaJpaRepository sagaRepository;

    private Long memberId;

    private PlaceOrderResult placeOrder() {
        memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        Long memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);
        return placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000));
    }

    private ConfirmPaymentCommand confirmCommand(PlaceOrderResult placed, String paymentKey) {
        return new ConfirmPaymentCommand(memberId, placed.getOrderNo(), paymentKey, placed.getPayAmount());
    }

    private OrderSaga findSaga(PlaceOrderResult placed) {
        Long orderId = orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow().getId();
        return sagaRepository.findByOrderId(orderId).orElseThrow();
    }

    @Test
    @DisplayName("결제가 진행 중인 주문에 승인 요청이 다시 오면 PG를 부르지 않고 거부한다")
    void secondRequestIsRejected() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willTimeout();
        paymentConfirm.confirm(confirmCommand(placed, "pay-key-1"));

        assertErrorCode(() -> paymentConfirm.confirm(confirmCommand(placed, "pay-key-2")),
                ErrorCode.PAYMENT_IN_PROGRESS);

        assertThat(paymentGateway.callCount()).isEqualTo(1);
        assertThat(findSaga(placed).getPaymentKey()).isEqualTo("pay-key-1");
    }

}