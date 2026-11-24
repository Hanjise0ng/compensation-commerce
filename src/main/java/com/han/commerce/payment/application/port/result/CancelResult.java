package com.han.commerce.payment.application.port.result;

public sealed interface CancelResult permits Canceled, CancelFailed {
}