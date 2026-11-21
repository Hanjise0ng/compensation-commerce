package com.han.commerce.order.application;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.domain.SagaStatus;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.order.infrastructure.OrderSagaJpaRepository;
import com.han.commerce.payment.domain.PaymentStatus;
import com.han.commerce.payment.infrastructure.PaymentJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.infrastructure.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentCancelCompensationTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderSagaJpaRepository sagaRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    private Long memberId;
    private Product product;
    private Long memberCouponId;

    private PlaceOrderResult placeOrder() {
        memberId = fixtures.member();
        product = fixtures.product("키보드", 10_000, 10);
        memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);
        return placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000));
    }

    private ConfirmPaymentCommand confirmCommand(PlaceOrderResult placed) {
        return new ConfirmPaymentCommand(memberId, placed.getOrderNo(), "pay-key-1", placed.getPayAmount());
    }

    private Order findOrder(PlaceOrderResult placed) {
        return orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow();
    }

    private OrderSaga findSaga(PlaceOrderResult placed) {
        return sagaRepository.findByOrderId(findOrder(placed).getId()).orElseThrow();
    }

    private Stock findStock() {
        return stockRepository.findByProductId(product.getId()).orElseThrow();
    }

    @Test
    @DisplayName("[문제 재현] PG는 승인했는데 주문 확정이 실패하면 결제만 남는다")
    void approvedButConfirmFails() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willApproveAmount(placed.getPayAmount() - 1_000);

        assertErrorCode(() -> paymentConfirm.confirm(confirmCommand(placed)), ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        assertThat(paymentGateway.callCount()).isEqualTo(1);
        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.READY);
        assertThat(findStock().getReserved()).isEqualTo(2);
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.STARTED);
    }

}