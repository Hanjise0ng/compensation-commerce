package com.han.commerce.payment.application.port.result;

public sealed interface PaymentResult permits Approved, Declined, Unknown {
}