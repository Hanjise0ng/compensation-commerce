package com.han.commerce.order.infrastructure;

import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends OrderRepository, JpaRepository<Order, Long> {

    @Override
    Optional<Order> findById(Long id);

    @Override
    @Query(value = """
            SELECT id
            FROM orders
            WHERE expires_at <= :now
              AND status = 'PENDING'
            ORDER BY expires_at
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findExpiredOrderIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

}