package com.han.commerce.payment.domain.refund;

import java.util.Optional;

public interface RefundRepository {

    Refund save(Refund refund);

    Optional<Refund> findByOrderId(Long orderId);

}