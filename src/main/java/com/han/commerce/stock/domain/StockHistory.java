package com.han.commerce.stock.domain;

import com.han.commerce.common.domain.BaseCreatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_history")
public class StockHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private StockHistoryType type;

    private int quantity;

    private int onHandAfter;

    private int reservedAfter;

    protected StockHistory() {
    }

    static StockHistory of(Stock stock, Long orderId, StockHistoryType type, int quantity) {
        StockHistory history = new StockHistory();
        history.productId = stock.getProductId();
        history.orderId = orderId;
        history.type = type;
        history.quantity = quantity;
        history.onHandAfter = stock.getOnHand();
        history.reservedAfter = stock.getReserved();
        return history;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public StockHistoryType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getOnHandAfter() {
        return onHandAfter;
    }

    public int getReservedAfter() {
        return reservedAfter;
    }

}