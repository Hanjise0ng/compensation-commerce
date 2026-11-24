package com.han.commerce.order.application.compensation;

import com.han.commerce.order.application.checkout.OrderPaymentSaga;
import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.application.recovery.OrderSagaService;
import com.han.commerce.order.application.recovery.PaymentRecoveryService;
import com.han.commerce.order.application.recovery.dto.RecoveryOutcome;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.saga.OrderSaga;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.domain.saga.SagaStatus;
import com.han.commerce.order.infrastructure.persistence.OrderJpaRepository;
import com.han.commerce.order.infrastructure.persistence.OrderSagaJpaRepository;
import com.han.commerce.payment.domain.refund.RefundStatus;
import com.han.commerce.payment.infrastructure.persistence.RefundJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.infrastructure.persistence.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCancelRetryTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private OrderSagaService sagaService;

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

    private Product product;

    private Long startFailingCancel() {
        Long memberId = fixtures.member();
        product = fixtures.product("키보드", 10_000, 10);
        PlaceOrderResult placed = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), null, 0));
        paymentGateway.willApproveAmount(placed.getPayAmount() - 1_000);
        paymentGateway.cancelWillFail("PG 점검 중");
        paymentConfirm.confirm(new ConfirmPaymentCommand(memberId, placed.getOrderNo(), "pay-key-1",
                placed.getPayAmount()));
        return orderRepository.findByOrderNo(placed.getOrderNo()).orElseThrow().getId();
    }

    private OrderSaga findSaga(Long orderId) {
        return sagaRepository.findByOrderId(orderId).orElseThrow();
    }

    @Test
    @DisplayName("취소가 실패할 때마다 다음 시도까지의 간격이 두 배로 늘어난다")
    void retryIntervalDoubles() {
        Long orderId = startFailingCancel();

        assertThat(findSaga(orderId).getNextCheckAt()).isEqualTo(clock.now().plusSeconds(10));
        assertThat(sagaService.findDueSagaIds(10)).isEmpty();

        for (int seconds : new int[] {20, 40, 80}) {
            clock.advance(Duration.between(clock.now(), findSaga(orderId).getNextCheckAt()));
            assertThat(sagaService.findDueSagaIds(10)).hasSize(1);

            RecoveryOutcome outcome = recoveryService.recover(findSaga(orderId).getId());

            assertThat(outcome).isEqualTo(RecoveryOutcome.CANCEL_POSTPONED);
            assertThat(findSaga(orderId).getNextCheckAt()).isEqualTo(clock.now().plusSeconds(seconds));
        }
        assertThat(findSaga(orderId).getAttemptCount()).isEqualTo(4);
        assertThat(paymentGateway.cancelCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("취소가 다섯 번 실패하면 자동 재시도를 멈추고 수동 처리 대상으로 남긴다")
    void givesUpAfterFiveFailures() {
        Long orderId = startFailingCancel();
        Long sagaId = findSaga(orderId).getId();

        RecoveryOutcome last = null;
        for (int i = 0; i < 4; i++) {
            clock.advance(Duration.between(clock.now(), findSaga(orderId).getNextCheckAt()));
            last = recoveryService.recover(sagaId);
        }

        assertThat(last).isEqualTo(RecoveryOutcome.CANCEL_GAVE_UP);
        OrderSaga saga = findSaga(orderId);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_FAILED);
        assertThat(saga.getAttemptCount()).isEqualTo(5);
        assertThat(saga.getLastError()).isEqualTo("PG 점검 중");
        assertThat(saga.getNextCheckAt()).isNull();
        assertThat(paymentGateway.cancelCount()).isEqualTo(5);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(2);
        assertThat(refundRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(RefundStatus.REQUESTED);

        clock.advance(Duration.ofDays(1));
        assertThat(sagaService.findDueSagaIds(10)).isEmpty();
        assertThat(recoveryService.recover(sagaId)).isEqualTo(RecoveryOutcome.SKIPPED);
        assertThat(paymentGateway.cancelCount()).isEqualTo(5);
    }

}