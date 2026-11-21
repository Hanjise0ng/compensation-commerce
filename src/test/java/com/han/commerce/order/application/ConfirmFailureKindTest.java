package com.han.commerce.order.application;

import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.domain.SagaStatus;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.order.infrastructure.OrderSagaJpaRepository;
import com.han.commerce.payment.infrastructure.RefundJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.infrastructure.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmFailureKindTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private PaymentRecoveryService recoveryService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderSagaJpaRepository sagaRepository;

    @Autowired
    private RefundJpaRepository refundRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Test
    @DisplayName("확정 중 일시적 오류가 나면 결제를 취소하지 않고, 복구 워커가 다시 확정한다")
    void transientFailureIsRetriedNotCanceled() {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        PlaceOrderResult placed = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), null, 0));
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(memberId, placed.getOrderNo(), "pay-key-1",
                placed.getPayAmount());
        stockCommitter.failNextCommit();

        assertThatThrownBy(() -> paymentConfirm.confirm(command)).isInstanceOf(IllegalStateException.class);

        Long orderId = orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow().getId();
        assertThat(paymentGateway.callCount()).isEqualTo(1);
        assertThat(paymentGateway.cancelCount()).isEqualTo(0);
        assertThat(refundRepository.findByOrderId(orderId)).isEmpty();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        OrderSaga saga = sagaRepository.findByOrderId(orderId).orElseThrow();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);

        clock.advance(Duration.ofMinutes(1));
        RecoveryOutcome outcome = recoveryService.recover(saga.getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.CONFIRMED);
        assertThat(paymentGateway.cancelCount()).isEqualTo(0);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getOnHand()).isEqualTo(8);
        assertThat(stock.getReserved()).isEqualTo(0);
        assertThat(sagaRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

}