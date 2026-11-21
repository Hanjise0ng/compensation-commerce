package com.han.commerce.order.infrastructure;

import com.han.commerce.order.domain.OrderSaga;
import com.han.commerce.order.domain.OrderSagaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderSagaJpaRepository extends OrderSagaRepository, JpaRepository<OrderSaga, Long> {

    @Override
    Optional<OrderSaga> findById(Long id);

    @Override
    @Query(value = """
            SELECT id
            FROM order_saga
            WHERE next_check_at <= :now
            ORDER BY next_check_at
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

}