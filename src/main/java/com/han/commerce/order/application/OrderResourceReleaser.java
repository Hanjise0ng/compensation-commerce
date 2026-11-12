package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.coupon.domain.MemberCoupon;
import com.han.commerce.coupon.domain.MemberCouponRepository;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderItem;
import com.han.commerce.point.domain.PointHistoryRepository;
import com.han.commerce.point.domain.PointWallet;
import com.han.commerce.point.domain.PointWalletRepository;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * 결제 대기 주문이 선점한 자원(재고 예약, 쿠폰, 포인트)을 되돌린다.
 * 호출하는 쪽의 트랜잭션 안에서 실행된다.
 */
@Component
public class OrderResourceReleaser {

    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public OrderResourceReleaser(StockRepository stockRepository,
                                 StockHistoryRepository stockHistoryRepository,
                                 MemberCouponRepository memberCouponRepository,
                                 PointWalletRepository pointWalletRepository,
                                 PointHistoryRepository pointHistoryRepository) {
        this.stockRepository = stockRepository;
        this.stockHistoryRepository = stockHistoryRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.pointWalletRepository = pointWalletRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    public void release(Order order) {
        releaseStocks(order);
        restoreCoupon(order);
        restorePoint(order);
    }

    private void releaseStocks(Order order) {
        TreeSet<Long> productIds = new TreeSet<>();
        for (OrderItem item : order.getItems()) {
            productIds.add(item.getProductId());
        }
        Map<Long, Stock> stocks = new HashMap<>();
        for (Stock stock : stockRepository.findAllByProductIdsForUpdate(productIds)) {
            stocks.put(stock.getProductId(), stock);
        }
        for (OrderItem item : order.getItems()) {
            Stock stock = stocks.get(item.getProductId());
            if (stock == null) {
                throw new DomainException(ErrorCode.STOCK_NOT_FOUND, "상품 " + item.getProductId());
            }
            stockHistoryRepository.save(stock.release(item.getQuantity(), order.getId()));
        }
    }

    private void restoreCoupon(Order order) {
        if (!order.usesCoupon()) {
            return;
        }
        MemberCoupon memberCoupon = memberCouponRepository.findById(order.getMemberCouponId())
                .orElseThrow(() -> new DomainException(ErrorCode.COUPON_NOT_FOUND));
        memberCoupon.restore();
    }

    private void restorePoint(Order order) {
        if (!order.usesPoint()) {
            return;
        }
        PointWallet wallet = pointWalletRepository.findByMemberId(order.getMemberId())
                .orElseThrow(() -> new DomainException(ErrorCode.POINT_WALLET_NOT_FOUND));
        pointHistoryRepository.save(wallet.restore(order.getPointAmount(), order.getId()));
    }

}