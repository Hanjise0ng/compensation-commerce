package com.han.commerce.order.application.placement;

import com.han.commerce.common.application.port.ClockHolder;
import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.application.CouponService;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.application.placement.port.OrderNoGenerator;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderItem;
import com.han.commerce.order.domain.OrderRepository;
import com.han.commerce.payment.application.PaymentService;
import com.han.commerce.point.application.PointService;
import com.han.commerce.product.application.ProductQueryService;
import com.han.commerce.product.application.dto.ProductInfo;
import com.han.commerce.stock.application.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderPlacementService {

    private final ProductQueryService productQueryService;
    private final StockService stockService;
    private final CouponService couponService;
    private final PointService pointService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final OrderNoGenerator orderNoGenerator;
    private final ClockHolder clockHolder;
    private final Duration paymentWindow;

    public OrderPlacementService(ProductQueryService productQueryService,
                                 StockService stockService,
                                 CouponService couponService,
                                 PointService pointService,
                                 PaymentService paymentService,
                                 OrderRepository orderRepository,
                                 OrderNoGenerator orderNoGenerator,
                                 ClockHolder clockHolder,
                                 @Value("${commerce.order.payment-window:PT15M}") Duration paymentWindow) {
        this.productQueryService = productQueryService;
        this.stockService = stockService;
        this.couponService = couponService;
        this.pointService = pointService;
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.orderNoGenerator = orderNoGenerator;
        this.clockHolder = clockHolder;
        this.paymentWindow = paymentWindow;
    }

    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command) {
        LocalDateTime now = clockHolder.now();

        List<OrderItem> items = createItems(command);
        Money discount = useCoupon(command, OrderItem.totalOf(items), now);

        Order order = orderRepository.save(Order.place(
                orderNoGenerator.generate(), command.getIdempotencyKey(), command.getMemberId(), items,
                command.getMemberCouponId(), discount, Money.of(command.getPointAmount()),
                now, paymentWindow));

        stockService.reserve(order.getId(), order.quantityByProductId());
        if (order.usesPoint()) {
            pointService.use(order.getMemberId(), order.getPointAmount(), order.getId());
        }
        paymentService.prepare(order.getId(), order.getPayAmount());

        return PlaceOrderResult.from(order);
    }

    private List<OrderItem> createItems(PlaceOrderCommand command) {
        List<Long> productIds = command.getItems().stream().map(PlaceOrderCommand.Item::getProductId).toList();
        Map<Long, ProductInfo> products = productQueryService.getOnSaleProducts(productIds);

        List<OrderItem> items = new ArrayList<>();
        for (PlaceOrderCommand.Item item : command.getItems()) {
            ProductInfo product = products.get(item.getProductId());
            items.add(OrderItem.of(product.getId(), product.getName(), product.getPrice(), item.getQuantity()));
        }
        return items;
    }

    private Money useCoupon(PlaceOrderCommand command, Money orderAmount, LocalDateTime now) {
        if (command.getMemberCouponId() == null) {
            return Money.ZERO;
        }
        return couponService.use(command.getMemberCouponId(), command.getMemberId(), orderAmount, now);
    }

}