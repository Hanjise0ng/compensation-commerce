package com.han.commerce.payment.application;

public sealed interface CancelResult permits Canceled, CancelFailed {
}