package com.han.commerce.order.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private String idempotencyKey;

    private Long memberId;

    private Long memberCouponId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "discount_amount", nullable = false))
    private Money discountAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "point_amount", nullable = false))
    private Money pointAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "pay_amount", nullable = false))
    private Money payAmount;

    private String failureReason;

    private LocalDateTime orderedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public static Order place(String orderNo, String idempotencyKey, Long memberId, List<OrderItem> items,
                              Long memberCouponId, Money discount, Money point,
                              LocalDateTime now, Duration paymentWindow) {
        if (items.isEmpty()) {
            throw new DomainException(ErrorCode.EMPTY_ORDER);
        }
        Order order = new Order();
        order.orderNo = orderNo;
        order.idempotencyKey = idempotencyKey;
        order.memberId = memberId;
        order.memberCouponId = memberCouponId;
        order.status = OrderStatus.PENDING;
        order.addItems(items);
        order.totalAmount = OrderItem.totalOf(items);
        order.discountAmount = discount;
        order.pointAmount = point;
        order.payAmount = order.totalAmount.minus(discount).minus(point);
        order.orderedAt = now;
        order.expiresAt = now.plus(paymentWindow);
        return order;
    }

    private void addItems(List<OrderItem> newItems) {
        Set<Long> productIds = new HashSet<>();
        for (OrderItem item : newItems) {
            if (!productIds.add(item.getProductId())) {
                throw new DomainException(ErrorCode.DUPLICATE_ORDER_ITEM, "상품 " + item.getProductId());
            }
            item.assignTo(this);
            this.items.add(item);
        }
    }

    public void validateOwner(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED, orderNo);
        }
    }

    public void startPayment(Long memberId, Money requestedAmount) {
        validateOwner(memberId);
        requireStatus(OrderStatus.PENDING);
        requireSameAmount(requestedAmount);
        this.expiresAt = null;
    }

    public void completePayment(Money approvedAmount, LocalDateTime now) {
        requireStatus(OrderStatus.PENDING);
        requireSameAmount(approvedAmount);
        this.status = OrderStatus.PAID;
        this.paidAt = now;
        this.expiresAt = null;
    }

    public void fail(String reason) {
        requireStatus(OrderStatus.PENDING);
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
        this.expiresAt = null;
    }

    public void expire(LocalDateTime now) {
        requireStatus(OrderStatus.PENDING);
        if (expiresAt == null || expiresAt.isAfter(now)) {
            throw new DomainException(ErrorCode.ORDER_NOT_EXPIRED, orderNo);
        }
        this.status = OrderStatus.EXPIRED;
        this.failureReason = "결제 기한 만료";
        this.expiresAt = null;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new DomainException(ErrorCode.INVALID_ORDER_STATE, orderNo + ", 현재 " + status);
        }
    }

    private void requireSameAmount(Money amount) {
        if (!payAmount.equals(amount)) {
            throw new DomainException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문 " + payAmount + ", 요청 " + amount);
        }
    }

    public boolean usesCoupon() {
        return memberCouponId != null;
    }

    public boolean usesPoint() {
        return !pointAmount.isZero();
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getMemberCouponId() {
        return memberCouponId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public Money getPointAmount() {
        return pointAmount;
    }

    public Money getPayAmount() {
        return payAmount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

}