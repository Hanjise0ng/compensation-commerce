package com.han.commerce.order.presentation;

import com.han.commerce.auth.AuthenticatedMember;
import com.han.commerce.auth.LoginMember;
import com.han.commerce.order.application.placement.OrderPlacementFacade;
import com.han.commerce.order.application.placement.dto.PlaceOrderResult;
import com.han.commerce.order.application.query.OrderQueryService;
import com.han.commerce.order.presentation.request.PlaceOrderRequest;
import com.han.commerce.order.presentation.response.OrderDetailResponse;
import com.han.commerce.order.presentation.response.PlaceOrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPlacementFacade placementFacade;
    private final OrderQueryService queryService;

    public OrderController(OrderPlacementFacade placementFacade, OrderQueryService queryService) {
        this.placementFacade = placementFacade;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> place(@LoginMember AuthenticatedMember member,
                                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderResult result = placementFacade.place(request.toCommand(member.getMemberId(), idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaceOrderResponse.from(result));
    }

    @GetMapping("/{orderNo}")
    public OrderDetailResponse get(@LoginMember AuthenticatedMember member, @PathVariable String orderNo) {
        return OrderDetailResponse.from(queryService.getOrder(member.getMemberId(), orderNo));
    }

}