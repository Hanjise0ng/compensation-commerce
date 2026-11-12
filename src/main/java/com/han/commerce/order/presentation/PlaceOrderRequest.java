package com.han.commerce.order.presentation;

import com.han.commerce.order.application.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public class PlaceOrderRequest {

    @NotEmpty
    @Valid
    private List<Item> items;

    private Long memberCouponId;

    @PositiveOrZero
    private long pointAmount;

    protected PlaceOrderRequest() {
    }

    public PlaceOrderCommand toCommand(Long memberId, String idempotencyKey) {
        List<PlaceOrderCommand.Item> commandItems = items.stream()
                .map(item -> new PlaceOrderCommand.Item(item.getProductId(), item.getQuantity()))
                .toList();
        return new PlaceOrderCommand(memberId, idempotencyKey, commandItems, memberCouponId, pointAmount);
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

        @NotNull
        private Long productId;

        @Positive
        private int quantity;

        protected Item() {
        }

        public Long getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

}