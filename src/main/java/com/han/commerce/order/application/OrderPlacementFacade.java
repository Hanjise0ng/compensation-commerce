package com.han.commerce.order.application;

import com.han.commerce.order.domain.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderPlacementFacade {

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
        try {
            return placementService.place(command);
        } catch (DataIntegrityViolationException e) {
            return findExisting(command).orElseThrow(() -> e);
        }
    }

    private Optional<PlaceOrderResult> findExisting(PlaceOrderCommand command) {
        return orderRepository
                .findByMemberIdAndIdempotencyKey(command.getMemberId(), command.getIdempotencyKey())
                .map(PlaceOrderResult::from);
    }

}