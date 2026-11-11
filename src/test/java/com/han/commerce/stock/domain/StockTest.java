package com.han.commerce.stock.domain;

import com.han.commerce.common.domain.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.han.commerce.support.DomainAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;

class StockTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long ORDER_ID = 100L;

    private Stock stockOf(int onHand) {
        Stock stock = Stock.create(PRODUCT_ID);
        stock.receive(onHand);
        return stock;
    }

    @Test
    @DisplayName("예약하면 보유 수량은 그대로이고 판매 가능 수량만 준다")
    void reserve() {
        Stock stock = stockOf(10);

        StockHistory history = stock.reserve(3, ORDER_ID);

        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(3);
        assertThat(stock.available()).isEqualTo(7);
        assertThat(history.getType()).isEqualTo(StockHistoryType.RESERVE);
        assertThat(history.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(history.getReservedAfter()).isEqualTo(3);
    }

    @Test
    @DisplayName("판매 가능 수량보다 많이 예약할 수 없다")
    void reserveMoreThanAvailable() {
        Stock stock = stockOf(10);
        stock.reserve(8, ORDER_ID);

        assertErrorCode(() -> stock.reserve(3, 101L), ErrorCode.OUT_OF_STOCK);
        assertThat(stock.getReserved()).isEqualTo(8);
    }

    @Test
    @DisplayName("확정하면 보유 수량과 예약 수량이 함께 준다")
    void commit() {
        Stock stock = stockOf(10);
        stock.reserve(3, ORDER_ID);

        StockHistory history = stock.commit(3, ORDER_ID);

        assertThat(stock.getOnHand()).isEqualTo(7);
        assertThat(stock.getReserved()).isEqualTo(0);
        assertThat(history.getType()).isEqualTo(StockHistoryType.COMMIT);
    }

    @Test
    @DisplayName("예약을 해제하면 예약 수량만 준다 (reserve의 보상)")
    void release() {
        Stock stock = stockOf(10);
        stock.reserve(3, ORDER_ID);

        StockHistory history = stock.release(3, ORDER_ID);

        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(0);
        assertThat(history.getType()).isEqualTo(StockHistoryType.RELEASE);
    }

    @Test
    @DisplayName("예약한 수량보다 많이 해제하거나 확정할 수 없다")
    void cannotExceedReserved() {
        Stock stock = stockOf(10);
        stock.reserve(2, ORDER_ID);

        assertErrorCode(() -> stock.release(3, ORDER_ID), ErrorCode.INVALID_STOCK_STATE);
        assertErrorCode(() -> stock.commit(3, ORDER_ID), ErrorCode.INVALID_STOCK_STATE);
    }

    @Test
    @DisplayName("재입고하면 보유 수량이 는다 (commit의 보상)")
    void restock() {
        Stock stock = stockOf(10);
        stock.reserve(3, ORDER_ID);
        stock.commit(3, ORDER_ID);

        stock.restock(3, ORDER_ID);

        assertThat(stock.getOnHand()).isEqualTo(10);
        assertThat(stock.getReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("수량은 1 이상이어야 한다")
    void quantityMustBePositive() {
        Stock stock = stockOf(10);

        assertErrorCode(() -> stock.reserve(0, ORDER_ID), ErrorCode.INVALID_QUANTITY);
        assertErrorCode(() -> stock.receive(-1), ErrorCode.INVALID_QUANTITY);
    }

}