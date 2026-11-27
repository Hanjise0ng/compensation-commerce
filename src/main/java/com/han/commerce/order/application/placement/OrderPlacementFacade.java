package com.han.commerce.order.application.placement;

import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.order.application.placement.dto.PlaceOrderCommand;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderPlacementFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacementFacade.class);
    private static final int MAX_ATTEMPTS = 2;

    private final OrderPlacementService placementService;
    private final OrderRepository orderRepository;

    public OrderPlacementFacade(OrderPlacementService placementService, OrderRepository orderRepository) {
        this.placementService = placementService;
        this.orderRepository = orderRepository;
    }

    public PlaceOrderResult place(PlaceOrderCommand command) {
        Optional<PlaceOrderResult> existing = findExisting(command);
        if (existing.isPresent()) {
            return existing.get();
        }
        for (int attempt = 1; ; attempt++) {
            try {
                return placementService.place(command);
            } catch (DataIntegrityViolationException e) {
                Optional<PlaceOrderResult> duplicated = findExisting(command);
                if (duplicated.isPresent()) {
                    return duplicated.get();
                }
                log.warn("주문 생성 중 제약 위반 attempt={} reason={}", attempt, e.getMessage());
                if (attempt >= MAX_ATTEMPTS) {
                    throw new DomainException(ErrorCode.ORDER_NO_CONFLICT);
                }
            }
        }
    }

    private Optional<PlaceOrderResult> findExisting(PlaceOrderCommand command) {
        return orderRepository
                .findByMemberIdAndIdempotencyKey(command.getMemberId(), command.getIdempotencyKey())
                .map(PlaceOrderResult::from);
    }

}