package com.han.commerce.order.application;

import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.MemberCouponStatus;
import com.han.commerce.coupon.infrastructure.MemberCouponJpaRepository;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.domain.SagaStatus;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.order.infrastructure.OrderSagaJpaRepository;
import com.han.commerce.payment.domain.PaymentStatus;
import com.han.commerce.payment.infrastructure.PaymentJpaRepository;
import com.han.commerce.point.infrastructure.PointWalletJpaRepository;
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

class PaymentRecoveryTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private PaymentAttemptService attemptService;

    @Autowired
    private OrderSagaService sagaService;

    @Autowired
    private PaymentRecoveryService recoveryService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderSagaJpaRepository sagaRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private MemberCouponJpaRepository memberCouponRepository;

    @Autowired
    private PointWalletJpaRepository pointWalletRepository;

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

    private PlaceOrderResult placeOrderAndTimeout() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willTimeout();
        paymentConfirm.confirm(confirmCommand(placed));
        return placed;
    }

    @Test
    @DisplayName("결제 시도 후 1분이 지나기 전에는 복구 대상이 아니다")
    void notDueYet() {
        placeOrderAndTimeout();

        clock.advance(Duration.ofSeconds(59));
        assertThat(sagaService.findDueSagaIds(10)).isEmpty();

        clock.advance(Duration.ofSeconds(1));
        assertThat(sagaService.findDueSagaIds(10)).hasSize(1);
    }

    @Test
    @DisplayName("응답을 못 받았지만 PG에서는 승인된 결제를 복구하면 주문이 확정된다")
    void recoversApprovedPayment() {
        PlaceOrderResult placed = placeOrderAndTimeout();
        clock.advance(Duration.ofMinutes(1));
        paymentGateway.inquiryWillFindApproved();

        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.CONFIRMED);
        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
        assertThat(findStock().getOnHand()).isEqualTo(8);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(sagaService.findDueSagaIds(10)).isEmpty();
    }

    @Test
    @DisplayName("PG에 결제 기록이 없으면 복구가 보상을 실행해 자원을 돌려준다")
    void compensatesWhenPaymentNotFound() {
        PlaceOrderResult placed = placeOrderAndTimeout();
        clock.advance(Duration.ofMinutes(1));
        paymentGateway.inquiryWillFindNothing();

        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.COMPENSATED);
        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(findStock().getOnHand()).isEqualTo(10);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    @DisplayName("PG 조회도 답이 없으면 아무것도 바꾸지 않고 다음 확인으로 미룬다")
    void postponesWhenInquiryTimesOut() {
        PlaceOrderResult placed = placeOrderAndTimeout();
        clock.advance(Duration.ofMinutes(1));
        paymentGateway.inquiryWillTimeout();

        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.POSTPONED);
        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(findStock().getReserved()).isEqualTo(2);
        OrderSaga saga = findSaga(placed);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.getAttemptCount()).isEqualTo(1);
        assertThat(saga.getLastError()).isEqualTo("inquiry timeout");
        assertThat(saga.getNextCheckAt()).isEqualTo(clock.now().plus(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("결제 시도만 커밋되고 서버가 멈춘 주문도 복구된다")
    void recoversAfterCrashBetweenAttemptAndConfirm() {
        PlaceOrderResult placed = placeOrder();
        attemptService.start(confirmCommand(placed));
        assertThat(paymentGateway.callCount()).isEqualTo(0);

        clock.advance(Duration.ofMinutes(1));
        paymentGateway.inquiryWillFindApproved();
        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.CONFIRMED);
        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(findStock().getOnHand()).isEqualTo(8);
    }

    @Test
    @DisplayName("이미 끝난 사가를 복구하면 PG에 묻지 않고 건너뛴다")
    void skipsFinishedSaga() {
        PlaceOrderResult placed = placeOrder();
        paymentConfirm.confirm(confirmCommand(placed));

        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.SKIPPED);
        assertThat(paymentGateway.inquiryCount()).isEqualTo(0);
        assertThat(findStock().getOnHand()).isEqualTo(8);
    }

}