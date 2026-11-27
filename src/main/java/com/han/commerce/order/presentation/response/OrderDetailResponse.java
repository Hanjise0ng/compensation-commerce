package com.han.commerce.order.presentation.response;

import com.han.commerce.order.application.query.dto.OrderDetail;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetailResponse {

    private final String orderNo;
    private final String status;
    private final long totalAmount;
    private final long discountAmount;
    private final long pointAmount;
    private final long payAmount;
    private final LocalDateTime orderedAt;
    private final LocalDateTime expiresAt;
    private final List<Item> items;

    private OrderDetailResponse(OrderDetail detail) {
        this.orderNo = detail.getOrderNo();
        this.status = detail.getStatus().name();
        this.totalAmount = detail.getTotalAmount();
        this.discountAmount = detail.getDiscountAmount();
        this.pointAmount = detail.getPointAmount();
        this.payAmount = detail.getPayAmount();
        this.orderedAt = detail.getOrderedAt();
        this.expiresAt = detail.getExpiresAt();
        this.items = detail.getItems().stream().map(Item::new).toList();
    }

    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(detail);
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

        private Item(OrderDetail.Item item) {
            this.productId = item.getProductId();
            this.productName = item.getProductName();
            this.unitPrice = item.getUnitPrice();
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