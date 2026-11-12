package com.han.commerce.order.application;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.MemberCouponStatus;
import com.han.commerce.coupon.infrastructure.MemberCouponJpaRepository;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.infrastructure.OrderJpaRepository;
import com.han.commerce.payment.domain.Payment;
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

import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class OrderPlacementTest extends IntegrationTestSupport {

    @Autowired
    private OrderPlacementFacade placementFacade;

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

    @Test
    @DisplayName("주문을 생성하면 재고 예약, 쿠폰 사용, 포인트 차감, 결제 준비가 함께 커밋된다")
    void place() {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        Long memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);

        PlaceOrderResult result = placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000));

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getPayAmount()).isEqualTo(15_000L);
        assertThat(result.getExpiresAt()).isEqualTo(clock.now().plusMinutes(15));

        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(2);

        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.USED);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(3_000));

        Order order = orderRepository.findByOrderNo(result.getOrderNo()).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getAmount()).isEqualTo(Money.of(15_000));
    }

    @Test
    @DisplayName("재고가 부족하면 쿠폰과 포인트도 쓰이지 않는다 (TX1 전체 롤백)")
    void rollbackWhenOutOfStock() {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 1);
        Long memberCouponId = fixtures.fixedCoupon(memberId, 3_000, 10_000, clock.now());
        fixtures.pointWallet(memberId, 5_000);

        assertErrorCode(() -> placementFacade.place(new PlaceOrderCommand(memberId, "key-1",
                        List.of(new PlaceOrderCommand.Item(product.getId(), 2)), memberCouponId, 2_000)),
                ErrorCode.OUT_OF_STOCK);

        assertThat(orderRepository.count()).isEqualTo(0L);
        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(0);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getStatus())
                .isEqualTo(MemberCouponStatus.AVAILABLE);
        assertThat(pointWalletRepository.findByMemberId(memberId).orElseThrow().getBalance())
                .isEqualTo(Money.of(5_000));
    }

    @Test
    @DisplayName("같은 멱등키로 다시 요청하면 같은 주문을 돌려주고 재고는 한 번만 예약된다")
    void idempotent() {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);
        PlaceOrderCommand command = new PlaceOrderCommand(memberId, "key-1",
                List.of(new PlaceOrderCommand.Item(product.getId(), 2)), null, 0);

        PlaceOrderResult first = placementFacade.place(command);
        PlaceOrderResult second = placementFacade.place(command);

        assertThat(second.getOrderNo()).isEqualTo(first.getOrderNo());
        assertThat(orderRepository.count()).isEqualTo(1L);
        assertThat(stockRepository.findByProductId(product.getId()).orElseThrow().getReserved()).isEqualTo(2);
    }

}