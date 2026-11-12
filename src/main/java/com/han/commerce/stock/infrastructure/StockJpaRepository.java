package com.han.commerce.stock.infrastructure;

import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockJpaRepository extends StockRepository, JpaRepository<Stock, Long> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productId in :productIds order by s.productId")
    List<Stock> findAllByProductIdsForUpdate(@Param("productIds") Collection<Long> productIds);

    Optional<Stock> findByProductId(Long productId);

}