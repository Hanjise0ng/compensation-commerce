package com.han.commerce.order.presentation;

import com.han.commerce.auth.AuthenticatedMember;
import com.han.commerce.auth.LoginMember;
import com.han.commerce.order.application.OrderPaymentSaga;
import com.han.commerce.order.application.PaymentConfirmResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final OrderPaymentSaga orderPaymentSaga;

    public PaymentController(OrderPaymentSaga orderPaymentSaga) {
        this.orderPaymentSaga = orderPaymentSaga;
    }

    @PostMapping("/confirm")
    public PaymentConfirmResult confirm(@LoginMember AuthenticatedMember member,
                                        @Valid @RequestBody ConfirmPaymentRequest request) {
        return orderPaymentSaga.confirm(request.toCommand(member.getMemberId()));
    }

}