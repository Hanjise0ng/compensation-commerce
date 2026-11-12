package com.han.commerce.order.application;

import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderItem;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetail {

    private final String orderNo;
    private final String status;
    private final long totalAmount;
    private final long discountAmount;
    private final long pointAmount;
    private final long payAmount;
    private final LocalDateTime orderedAt;
    private final LocalDateTime expiresAt;
    private final List<Item> items;

    private OrderDetail(Order order) {
        this.orderNo = order.getOrderNo();
        this.status = order.getStatus().name();
        this.totalAmount = order.getTotalAmount().getAmount();
        this.discountAmount = order.getDiscountAmount().getAmount();
        this.pointAmount = order.getPointAmount().getAmount();
        this.payAmount = order.getPayAmount().getAmount();
        this.orderedAt = order.getOrderedAt();
        this.expiresAt = order.getExpiresAt();
        this.items = order.getItems().stream().map(Item::new).toList();
    }

    public static OrderDetail from(Order order) {
        return new OrderDetail(order);
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getStatus() {
        return status;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getPointAmount() {
        return pointAmount;
    }

    public long getPayAmount() {
        return payAmount;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        private final Long productId;
        private final String productName;
        private final long unitPrice;
        private final int quantity;

        private Item(OrderItem item) {
            this.productId = item.getProductId();
            this.productName = item.getProductName();
            this.unitPrice = item.getUnitPrice().getAmount();
            this.quantity = item.getQuantity();
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public long getUnitPrice() {
            return unitPrice;
        }

        public int getQuantity() {
            return quantity;
        }
    }

}