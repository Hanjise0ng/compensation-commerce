package com.han.commerce.order.application;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.Coupon;
import com.han.commerce.coupon.domain.CouponRepository;
import com.han.commerce.coupon.domain.MemberCoupon;
import com.han.commerce.coupon.domain.MemberCouponRepository;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderItem;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import com.han.commerce.point.domain.PointHistoryRepository;
import com.han.commerce.point.domain.PointWallet;
import com.han.commerce.point.domain.PointWalletRepository;
import com.han.commerce.product.domain.Product;
import com.han.commerce.product.domain.ProductRepository;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderPlacementService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderNoGenerator orderNoGenerator;
    private final ClockHolder clockHolder;
    private final Duration paymentWindow;

    public OrderPlacementService(ProductRepository productRepository,
                                 StockRepository stockRepository,
                                 StockHistoryRepository stockHistoryRepository,
                                 CouponRepository couponRepository,
                                 MemberCouponRepository memberCouponRepository,
                                 PointWalletRepository pointWalletRepository,
                                 PointHistoryRepository pointHistoryRepository,
                                 OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 OrderNoGenerator orderNoGenerator,
                                 ClockHolder clockHolder,
                                 @Value("${commerce.order.payment-window:PT15M}") Duration paymentWindow) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.stockHistoryRepository = stockHistoryRepository;
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.pointWalletRepository = pointWalletRepository;
        this.pointHistoryRepository = pointHistoryRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderNoGenerator = orderNoGenerator;
        this.clockHolder = clockHolder;
        this.paymentWindow = paymentWindow;
    }

    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command) {
        LocalDateTime now = clockHolder.now();

        List<OrderItem> items = createItems(command);
        Map<Long, Stock> stocks = lockStocks(items);
        Money discount = useCoupon(command, OrderItem.totalOf(items), now);

        Order order = orderRepository.save(Order.place(
                orderNoGenerator.generate(), command.getIdempotencyKey(), command.getMemberId(), items,
                command.getMemberCouponId(), discount, Money.of(command.getPointAmount()),
                now, paymentWindow));

        reserveStocks(order, stocks);
        usePoint(order);
        paymentRepository.save(Payment.ready(order.getId(), order.getPayAmount()));

        return PlaceOrderResult.from(order);
    }

    private List<OrderItem> createItems(PlaceOrderCommand command) {
        List<Long> productIds = command.getItems().stream().map(PlaceOrderCommand.Item::getProductId).toList();
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            products.put(product.getId(), product);
        }

        List<OrderItem> items = new ArrayList<>();
        for (PlaceOrderCommand.Item item : command.getItems()) {
            Product product = products.get(item.getProductId());
            if (product == null) {
                throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "상품 " + item.getProductId());
            }
            product.validateOnSale();
            items.add(OrderItem.of(product.getId(), product.getName(), product.getPrice(), item.getQuantity()));
        }
        return items;
    }

    private Map<Long, Stock> lockStocks(List<OrderItem> items) {
        TreeSet<Long> productIds = new TreeSet<>();
        for (OrderItem item : items) {
            productIds.add(item.getProductId());
        }
        Map<Long, Stock> stocks = new HashMap<>();
        for (Stock stock : stockRepository.findAllByProductIdsForUpdate(productIds)) {
            stocks.put(stock.getProductId(), stock);
        }
        if (stocks.size() != productIds.size()) {
            throw new DomainException(ErrorCode.STOCK_NOT_FOUND);
        }
        return stocks;
    }

    private Money useCoupon(PlaceOrderCommand command, Money orderAmount, LocalDateTime now) {
        if (command.getMemberCouponId() == null) {
            return Money.ZERO;
        }
        MemberCoupon memberCoupon = memberCouponRepository.findById(command.getMemberCouponId())
                .orElseThrow(() -> new DomainException(ErrorCode.COUPON_NOT_FOUND));
        Coupon coupon = couponRepository.findById(memberCoupon.getCouponId())
                .orElseThrow(() -> new DomainException(ErrorCode.COUPON_NOT_FOUND));

        Money discount = coupon.calculateDiscount(orderAmount, now);
        memberCoupon.use(command.getMemberId(), now);
        return discount;
    }

    private void reserveStocks(Order order, Map<Long, Stock> stocks) {
        for (OrderItem item : order.getItems()) {
            Stock stock = stocks.get(item.getProductId());
            stockHistoryRepository.save(stock.reserve(item.getQuantity(), order.getId()));
        }
    }

    private void usePoint(Order order) {
        if (!order.usesPoint()) {
            return;
        }
        PointWallet wallet = pointWalletRepository.findByMemberId(order.getMemberId())
                .orElseThrow(() -> new DomainException(ErrorCode.POINT_WALLET_NOT_FOUND));
        pointHistoryRepository.save(wallet.use(order.getPointAmount(), order.getId()));
    }

}