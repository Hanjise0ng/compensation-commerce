package com.han.commerce.order.presentation;

import com.han.commerce.auth.AuthenticatedMember;
import com.han.commerce.auth.LoginMember;
import com.han.commerce.order.application.OrderDetail;
import com.han.commerce.order.application.OrderPlacementFacade;
import com.han.commerce.order.application.OrderQueryService;
import com.han.commerce.order.application.PlaceOrderResult;
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
    public ResponseEntity<PlaceOrderResult> place(@LoginMember AuthenticatedMember member,
                                                  @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderResult result = placementFacade.place(request.toCommand(member.getMemberId(), idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{orderNo}")
    public OrderDetail get(@LoginMember AuthenticatedMember member, @PathVariable String orderNo) {
        return queryService.getOrder(member.getMemberId(), orderNo);
    }

}