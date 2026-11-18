package com.han.commerce.payment.application;

public sealed interface PaymentResult permits Approved, Declined, Unknown {
}