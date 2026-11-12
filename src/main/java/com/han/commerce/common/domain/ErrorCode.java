package com.han.commerce.common.domain;

public enum ErrorCode {

    INVALID_MONEY(ErrorType.INVALID, "금액은 음수일 수 없습니다."),
    INVALID_QUANTITY(ErrorType.INVALID, "수량은 1 이상이어야 합니다."),
    INVALID_COUPON(ErrorType.INVALID, "쿠폰 정책 값이 올바르지 않습니다."),

    UNAUTHENTICATED(ErrorType.UNAUTHENTICATED, "인증 정보가 없습니다."),

    PRODUCT_NOT_FOUND(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_ON_SALE(ErrorType.CONFLICT, "판매 중인 상품이 아닙니다."),

    STOCK_NOT_FOUND(ErrorType.NOT_FOUND, "재고 정보를 찾을 수 없습니다."),
    OUT_OF_STOCK(ErrorType.CONFLICT, "재고가 부족합니다."),
    INVALID_STOCK_STATE(ErrorType.CONFLICT, "예약 수량보다 많은 수량을 처리할 수 없습니다."),

    COUPON_NOT_FOUND(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_NOT_OWNED(ErrorType.FORBIDDEN, "본인 쿠폰이 아닙니다."),
    COUPON_ALREADY_USED(ErrorType.CONFLICT, "이미 사용한 쿠폰입니다."),
    COUPON_NOT_USED(ErrorType.CONFLICT, "사용 상태의 쿠폰이 아닙니다."),
    COUPON_NOT_APPLICABLE(ErrorType.CONFLICT, "쿠폰을 적용할 수 없습니다."),

    POINT_WALLET_NOT_FOUND(ErrorType.NOT_FOUND, "포인트 지갑을 찾을 수 없습니다."),
    INSUFFICIENT_POINT(ErrorType.CONFLICT, "포인트 잔액이 부족합니다."),

    EMPTY_ORDER(ErrorType.INVALID, "주문 항목이 비어 있습니다."),
    DUPLICATE_ORDER_ITEM(ErrorType.INVALID, "같은 상품이 주문 항목에 중복되었습니다."),
    ORDER_NOT_FOUND(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(ErrorType.FORBIDDEN, "본인 주문이 아닙니다."),
    INVALID_ORDER_STATE(ErrorType.CONFLICT, "현재 주문 상태에서는 처리할 수 없습니다."),
    ORDER_NOT_EXPIRED(ErrorType.CONFLICT, "결제 기한이 지나지 않은 주문입니다."),

    PAYMENT_NOT_FOUND(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(ErrorType.CONFLICT, "결제 금액이 주문 금액과 다릅니다."),
    INVALID_PAYMENT_STATE(ErrorType.CONFLICT, "현재 결제 상태에서는 처리할 수 없습니다.");

    private final ErrorType type;
    private final String message;

    ErrorCode(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }

    public ErrorType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

}