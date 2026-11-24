package com.han.commerce.order.application.checkout;

import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.MemberCouponStatus;
import com.han.commerce.coupon.infrastructure.persistence.MemberCouponJpaRepository;
import com.han.commerce.order.application.checkout.dto.ConfirmPaymentCommand;
import com.han.commerce.order.application.checkout.dto.PaymentConfirmResult;
import com.han.commerce.order.application.checkout.dto.PaymentConfirmStatus;
import com.han.commerce.order.application.expiration.OrderExpirationService;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.infrastructure.persistence.OrderJpaRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentStatus;
import com.han.commerce.payment.infrastructure.persistence.PaymentJpaRepository;
import com.han.commerce.point.infrastructure.persistence.PointHistoryJpaRepository;
import com.han.commerce.point.infrastructure.persistence.PointWalletJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryType;
import com.han.commerce.stock.infrastructure.persistence.StockHistoryJpaRepository;
import com.han.commerce.stock.infrastructure.persistence.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderPaymentSaga paymentConfirm;

    @Autowired
    private OrderExpirationService expirationService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

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

    private Stock findStock() {
        return stockRepository.findByProductId(product.getId()).orElseThrow();
    }

    @Test
    @DisplayName("PG가 승인하면 주문은 PAID가 되고 재고 예약이 확정된다")
    void approved() {
        PlaceOrderResult placed = placeOrder();

        PaymentConfirmResult result = paymentConfirm.confirm(confirmCommand(placed));

        assertThat(result.getStatus()).isEqualTo(PaymentConfirmStatus.PAID);
        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPaymentKey()).isEqualTo("pay-key-1");

        assertThat(findStock().getOnHand()).isEqualTo(8);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(stockHistoryRepository.countByOrderIdAndType(order.getId(), StockHistoryType.COMMIT))
                .isEqualTo(1L);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.USED);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(3_000));
        assertThat(pointHistoryRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("요청 금액이 주문 금액과 다르면 PG를 호출하지 않고 거부한다")
    void amountMismatch() {
        PlaceOrderResult placed = placeOrder();
        ConfirmPaymentCommand tampered =
                new ConfirmPaymentCommand(memberId, placed.getOrderNo(), "pay-key-1", 100);

        assertErrorCode(() -> paymentConfirm.confirm(tampered), ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        assertThat(paymentGateway.callCount()).isEqualTo(0);
        assertThat(findOrder(placed).getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("PG가 거절하면 주문은 FAILED가 되고 재고 예약, 쿠폰, 포인트가 돌아온다")
    void declinedIsCompensated() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willDecline("REJECT_CARD_COMPANY", "한도 초과");

        PaymentConfirmResult result = paymentConfirm.confirm(confirmCommand(placed));

        assertThat(result.getStatus()).isEqualTo(PaymentConfirmStatus.FAILED);
        assertThat(result.getMessage()).isEqualTo("한도 초과");

        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("REJECT_CARD_COMPANY");

        assertThat(findStock().getOnHand()).isEqualTo(10);
        assertThat(findStock().getReserved()).isEqualTo(0);
        assertThat(stockHistoryRepository.countByOrderIdAndType(order.getId(), StockHistoryType.RELEASE))
                .isEqualTo(1L);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
        assertThat(pointHistoryRepository.count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("PG 응답을 받지 못하면 아무것도 되돌리지 않고 결제 대기 상태로 둔다")
    void unknownIsNotCompensated() {
        PlaceOrderResult placed = placeOrder();
        paymentGateway.willTimeout();

        PaymentConfirmResult result = paymentConfirm.confirm(confirmCommand(placed));

        assertThat(result.getStatus()).isEqualTo(PaymentConfirmStatus.PENDING);

        Order order = findOrder(placed);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.READY);
        assertThat(findStock().getReserved()).isEqualTo(2);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.USED);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(3_000));
    }

}