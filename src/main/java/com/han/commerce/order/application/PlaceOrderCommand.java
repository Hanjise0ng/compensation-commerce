package com.han.commerce.order.application;

import java.util.List;

public class PlaceOrderCommand {

    private final Long memberId;
    private final String idempotencyKey;
    private final List<Item> items;
    private final Long memberCouponId;
    private final long pointAmount;

    public PlaceOrderCommand(Long memberId, String idempotencyKey, List<Item> items,
                             Long memberCouponId, long pointAmount) {
        this.memberId = memberId;
        this.idempotencyKey = idempotencyKey;
        this.items = List.copyOf(items);
        this.memberCouponId = memberCouponId;
        this.pointAmount = pointAmount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public List<Item> getItems() {
        return items;
    }

    public Long getMemberCouponId() {
        return memberCouponId;
    }

    public long getPointAmount() {
        return pointAmount;
    }

    public static class Item {

        private final Long productId;
        private final int quantity;

        public Item(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

}