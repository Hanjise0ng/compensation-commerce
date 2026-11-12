package com.han.commerce.order.application;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.MemberCouponStatus;
import com.han.commerce.coupon.infrastructure.MemberCouponJpaRepository;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderStatus;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.payment.domain.PaymentStatus;
import com.han.commerce.payment.infrastructure.PaymentJpaRepository;
import com.han.commerce.point.infrastructure.PointWalletJpaRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.infrastructure.StockJpaRepository;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class OrderExpirationTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

    @Autowired
    private OrderExpirationService expirationService;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private MemberCouponJpaRepository memberCouponRepository;

    @Autowired
    private PointWalletJpaRepository pointWalletRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    private Long memberId;
    private Product product;
    private Long memberCouponId;

    private Long placeOrder() {
        memberId = fixtures.member();
        product = fixtures.product("키보드", 10_000, 10);
        memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);

        PlaceOrderResult result = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000));
        return orderRepository.findByOrderNo(result.getOrderNo()).orElseThrow().getId();
    }

    @Test
    @DisplayName("결제 기한이 지난 주문을 만료하면 재고 예약, 쿠폰, 포인트가 돌아온다")
    void expire() {
        Long orderId = placeOrder();
        clock.advance(Duration.ofMinutes(16));

        assertThat(expirationService.findExpiredOrderIds(50)).containsExactly(orderId);
        expirationService.expire(orderId);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(0);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(expirationService.findExpiredOrderIds(50)).isEmpty();
    }

    @Test
    @DisplayName("결제 기한 전에는 만료 대상이 아니고 만료할 수도 없다")
    void notYetExpired() {
        Long orderId = placeOrder();
        clock.advance(Duration.ofMinutes(14));

        assertThat(expirationService.findExpiredOrderIds(50)).isEmpty();
        assertErrorCode(() -> expirationService.expire(orderId), ErrorCode.ORDER_NOT_EXPIRED);
        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 주문을 두 번 만료해도 자원은 한 번만 돌아온다")
    void expireTwice() {
        Long orderId = placeOrder();
        clock.advance(Duration.ofMinutes(16));
        expirationService.expire(orderId);

        assertErrorCode(() -> expirationService.expire(orderId), ErrorCode.INVALID_ORDER_STATE);

        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(0);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
    }

}