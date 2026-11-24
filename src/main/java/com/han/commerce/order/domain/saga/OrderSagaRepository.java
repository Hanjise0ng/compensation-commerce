package com.han.commerce.order.domain.saga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderSagaRepository {

    OrderSaga save(OrderSaga saga);

    Optional<OrderSaga> findById(Long id);

    Optional<OrderSaga> findByOrderId(Long orderId);

    List<Long> findDueIds(LocalDateTime now, int limit);

}