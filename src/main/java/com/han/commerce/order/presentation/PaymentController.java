package com.han.commerce.order.presentation;

import com.han.commerce.auth.AuthenticatedMember;
import com.han.commerce.auth.LoginMember;
import com.han.commerce.order.application.checkout.OrderPaymentSaga;
import com.han.commerce.order.application.checkout.dto.PaymentConfirmResult;
import com.han.commerce.order.application.checkout.dto.PaymentConfirmStatus;
import com.han.commerce.order.presentation.request.ConfirmPaymentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<PaymentConfirmResult> confirm(@LoginMember AuthenticatedMember member,
                                                        @Valid @RequestBody ConfirmPaymentRequest request) {
        PaymentConfirmResult result = orderPaymentSaga.confirm(request.toCommand(member.getMemberId()));
        HttpStatus status = result.getStatus() == PaymentConfirmStatus.PENDING ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

}