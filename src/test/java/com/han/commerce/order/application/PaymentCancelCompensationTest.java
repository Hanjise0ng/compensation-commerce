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
import com.han.commerce.payment.domain.Refund;
import com.han.commerce.payment.domain.RefundStatus;
import com.han.commerce.payment.infrastructure.PaymentJpaRepository;
import com.han.commerce.payment.infrastructure.RefundJpaRepository;
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

class PaymentCancelCompensationTest extends IntegrationTestSupport {

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
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private RefundJpaRepository refundRepository;

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

    private Refund findRefund(PlaceOrderResult placed) {
        return refundRepository.findByOrderId(findOrder(placed).getId()).orElseThrow();
    }

    private PaymentStatus paymentStatus(PlaceOrderResult placed) {
        return paymentRepository.findByOrderId(findOrder(placed).getId()).orElseThrow().getStatus();
    }

    private Stock findStock() {
        return stockRepository.findByProductId(product.getId()).orElseThrow();
    }

    @Test
    @DisplayName("PG는 승인했는데 주문 확정이 실패하면 결제를 취소하고 자원을 돌려준다")
    void cancelsPaymentWhenConfirmFails() {
        PlaceOrderResult placed = placeOrder();
        long approvedAmount = placed.getPayAmount() - 1_000;
        paymentGateway.willApproveAmount(approvedAmount);

        PaymentConfirmResult result = paymentConfirm.confirm(confirmCommand(placed));

        assertThat(result.getStatus()).isEqualTo(PaymentConfirmStatus.FAILED);
        assertThat(paymentGateway.cancelCount()).isEqualTo(1);
        assertThat(paymentGateway.lastCanceledAmount()).isEqualTo(Money.of(approvedAmount));

        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(paymentStatus(placed)).isEqualTo(PaymentStatus.CANCELED);
        Refund refund = findRefund(placed);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getAmount()).isEqualTo(Money.of(approvedAmount));

        assertThat(findStock().getOnHand()).isEqualTo(10);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    @DisplayName("복구 워커가 승인을 확인했지만 확정할 수 없으면 워커가 결제를 취소한다")
    void recoveryCancelsWhenConfirmFails() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willTimeout();
        paymentConfirm.confirm(confirmCommand(placed));
        clock.advance(Duration.ofMinutes(1));
        paymentGateway.inquiryWillFindApprovedAmount(placed.getPayAmount() - 1_000);

        RecoveryOutcome outcome = recoveryService.recover(findSaga(placed).getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.CANCELED);
        assertThat(paymentGateway.cancelCount()).isEqualTo(1);
        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(paymentStatus(placed)).isEqualTo(PaymentStatus.CANCELED);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    @DisplayName("PG 취소가 실패하면 주문과 자원을 그대로 두고, 다음 시도에서 취소되면 마무리한다")
    void retriesCancelUntilItSucceeds() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willApproveAmount(placed.getPayAmount() - 1_000);
        paymentGateway.cancelWillFail("PG 점검 중");

        PaymentConfirmResult result = paymentConfirm.confirm(confirmCommand(placed));

        assertThat(result.getStatus()).isEqualTo(PaymentConfirmStatus.PENDING);
        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentStatus(placed)).isEqualTo(PaymentStatus.READY);
        assertThat(findRefund(placed).getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(findStock().getReserved()).isEqualTo(2);
        OrderSaga saga = findSaga(placed);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.getAttemptCount()).isEqualTo(1);
        assertThat(saga.getLastError()).isEqualTo("PG 점검 중");

        clock.advance(Duration.ofMinutes(1));
        paymentGateway.cancelWillSucceed();
        RecoveryOutcome outcome = recoveryService.recover(saga.getId());

        assertThat(outcome).isEqualTo(RecoveryOutcome.CANCELED);
        assertThat(paymentGateway.cancelCount()).isEqualTo(2);
        assertThat(paymentGateway.inquiryCount()).isEqualTo(0);
        assertThat(findOrder(placed).getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(findRefund(placed).getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(findSaga(placed).getStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

}