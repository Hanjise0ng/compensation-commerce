package com.han.commerce.stock.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int onHand;

    private int reserved;

    protected Stock() {
    }

    public static Stock create(Long productId) {
        Stock stock = new Stock();
        stock.productId = productId;
        stock.onHand = 0;
        stock.reserved = 0;
        return stock;
    }

    public int available() {
        return onHand - reserved;
    }

    public StockHistory receive(int quantity) {
        requirePositive(quantity);
        this.onHand += quantity;
        return StockHistory.of(this, null, StockHistoryType.INBOUND, quantity);
    }

    public StockHistory reserve(int quantity, Long orderId) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new DomainException(ErrorCode.OUT_OF_STOCK,
                    "상품 " + productId + ", 판매 가능 " + available() + ", 요청 " + quantity);
        }
        this.reserved += quantity;
        return StockHistory.of(this, orderId, StockHistoryType.RESERVE, quantity);
    }

    public StockHistory commit(int quantity, Long orderId) {
        requireReserved(quantity);
        this.onHand -= quantity;
        this.reserved -= quantity;
        return StockHistory.of(this, orderId, StockHistoryType.COMMIT, quantity);
    }

    public StockHistory release(int quantity, Long orderId) {
        requireReserved(quantity);
        this.reserved -= quantity;
        return StockHistory.of(this, orderId, StockHistoryType.RELEASE, quantity);
    }

    public StockHistory restock(int quantity, Long orderId) {
        requirePositive(quantity);
        this.onHand += quantity;
        return StockHistory.of(this, orderId, StockHistoryType.RESTOCK, quantity);
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, String.valueOf(quantity));
        }
    }

    private void requireReserved(int quantity) {
        requirePositive(quantity);
        if (reserved < quantity) {
            throw new DomainException(ErrorCode.INVALID_STOCK_STATE,
                    "상품 " + productId + ", 예약 " + reserved + ", 요청 " + quantity);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

}