package com.han.commerce.order.infrastructure;

import com.han.commerce.order.application.OrderExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "commerce.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);
    private static final int BATCH_SIZE = 50;

    private final OrderExpirationService expirationService;

    public OrderExpirationScheduler(OrderExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Scheduled(fixedDelayString = "${commerce.order.expiration-interval-ms:10000}")
    public void expireOrders() {
        for (Long orderId : expirationService.findExpiredOrderIds(BATCH_SIZE)) {
            try {
                expirationService.expire(orderId);
                log.info("주문 만료 처리 orderId={}", orderId);
            } catch (RuntimeException e) {
                log.warn("주문 만료 실패 orderId={} reason={}", orderId, e.getMessage());
            }
        }
    }

}