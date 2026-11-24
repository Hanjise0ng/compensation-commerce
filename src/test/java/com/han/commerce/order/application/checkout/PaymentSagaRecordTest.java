package com.han.commerce.order.application.checkout;

import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.domain.saga.OrderSaga;
import com.han.commerce.order.domain.saga.SagaStatus;
import com.han.commerce.order.infrastructure.persistence.OrderJpaRepository;
import com.han.commerce.order.infrastructure.persistence.OrderSagaJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSagaRecordTest extends IntegrationTestSupport {

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
    @DisplayName("PG 응답을 받지 못하면 사가가 진행 중으로 남고 1분 뒤가 첫 확인 시각이다")
    void sagaStaysStartedOnTimeout() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willTimeout();

        paymentConfirm.confirm(confirmCommand(placed, "pay-key-1"));

        OrderSaga saga = findSaga(placed);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.getPaymentKey()).isEqualTo("pay-key-1");
        assertThat(saga.getNextCheckAt()).isEqualTo(clock.now().plus(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("승인되면 사가는 완료, 거절되면 보상됨으로 끝난다")
    void sagaFinishesWithOrder() {
        PlaceOrderResult approved = placeOrder();
        paymentConfirm.confirm(confirmCommand(approved, "pay-key-1"));

        assertThat(findSaga(approved).getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(findSaga(approved).getNextCheckAt()).isNull();

        memberId = fixtures.member();
        Product product = fixtures.product("마우스", 5_000, 10);
        PlaceOrderResult declined = placementFacade.place(new PlaceOrderCommand(memberId, "key-2",
                List.of(new PlaceOrderCommand.Item(product.getId(), 1)), null, 0));
        paymentGateway.willDecline("REJECT_CARD_COMPANY", "한도 초과");
        paymentConfirm.confirm(confirmCommand(declined, "pay-key-2"));

        assertThat(findSaga(declined).getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(findSaga(declined).getNextCheckAt()).isNull();
    }

}