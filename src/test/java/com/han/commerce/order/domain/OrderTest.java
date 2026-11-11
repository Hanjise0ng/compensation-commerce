package com.han.commerce.order.domain;

import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 12, 0);
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Long MEMBER_ID = 1L;

    private List<OrderItem> items() {
        return List.of(
                OrderItem.of(1L, "키보드", Money.of(10_000), 2),
                OrderItem.of(2L, "마우스", Money.of(5_000), 1));
    }

    private Order pendingOrder() {
        return Order.place("ORD-1", "key-1", MEMBER_ID, items(), 7L,
                Money.of(3_000), Money.of(2_000), NOW, WINDOW);
    }

    @Test
    @DisplayName("주문을 만들면 금액이 계산되고 결제 대기 상태가 된다")
    void place() {
        Order order = pendingOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualTo(Money.of(25_000));
        assertThat(order.getPayAmount()).isEqualTo(Money.of(20_000));
        assertThat(order.getExpiresAt()).isEqualTo(NOW.plusMinutes(15));
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.usesCoupon()).isTrue();
        assertThat(order.usesPoint()).isTrue();
    }

    @Test
    @DisplayName("항목이 없거나 같은 상품이 중복되면 주문할 수 없다")
    void invalidItems() {
        assertErrorCode(() -> Order.place("ORD-1", "key-1", MEMBER_ID, List.of(), null,
                Money.ZERO, Money.ZERO, NOW, WINDOW), ErrorCode.EMPTY_ORDER);

        List<OrderItem> duplicated = List.of(
                OrderItem.of(1L, "키보드", Money.of(10_000), 1),
                OrderItem.of(1L, "키보드", Money.of(10_000), 1));
        assertErrorCode(() -> Order.place("ORD-1", "key-1", MEMBER_ID, duplicated, null,
                Money.ZERO, Money.ZERO, NOW, WINDOW), ErrorCode.DUPLICATE_ORDER_ITEM);
    }

    @Test
    @DisplayName("할인과 포인트의 합은 총액을 넘을 수 없다")
    void discountCannotExceedTotal() {
        assertErrorCode(() -> Order.place("ORD-1", "key-1", MEMBER_ID, items(), null,
                Money.of(20_000), Money.of(5_001), NOW, WINDOW), ErrorCode.INVALID_MONEY);
    }

    @Test
    @DisplayName("결제를 시작하면 만료 대상에서 빠진다")
    void startPayment() {
        Order order = pendingOrder();

        order.startPayment(MEMBER_ID, Money.of(20_000));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("본인이 아니거나 금액이 다르면 결제를 시작할 수 없다")
    void startPaymentRejected() {
        Order order = pendingOrder();

        assertErrorCode(() -> order.startPayment(2L, Money.of(20_000)), ErrorCode.ORDER_ACCESS_DENIED);
        assertErrorCode(() -> order.startPayment(MEMBER_ID, Money.of(100)), ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        assertThat(order.getExpiresAt()).isEqualTo(NOW.plusMinutes(15));
    }

    @Test
    @DisplayName("승인 금액이 맞으면 결제 완료가 된다")
    void completePayment() {
        Order order = pendingOrder();

        order.completePayment(Money.of(20_000), NOW.plusMinutes(1));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("이미 실패한 주문은 다시 실패 처리하거나 결제 완료할 수 없다")
    void failOnlyOnce() {
        Order order = pendingOrder();
        order.fail("카드 한도 초과");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertErrorCode(() -> order.fail("다시"), ErrorCode.INVALID_ORDER_STATE);
        assertErrorCode(() -> order.completePayment(Money.of(20_000), NOW), ErrorCode.INVALID_ORDER_STATE);
    }

    @Test
    @DisplayName("결제 기한이 지나야 만료할 수 있다")
    void expire() {
        Order order = pendingOrder();

        assertErrorCode(() -> order.expire(NOW.plusMinutes(14)), ErrorCode.ORDER_NOT_EXPIRED);

        order.expire(NOW.plusMinutes(15));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(order.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("결제가 시작된 주문은 만료할 수 없다")
    void cannotExpireAfterPaymentStarted() {
        Order order = pendingOrder();
        order.startPayment(MEMBER_ID, Money.of(20_000));

        assertErrorCode(() -> order.expire(NOW.plusHours(1)), ErrorCode.ORDER_NOT_EXPIRED);
    }

}