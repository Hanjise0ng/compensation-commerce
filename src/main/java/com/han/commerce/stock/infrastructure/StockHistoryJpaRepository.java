package com.han.commerce.stock.infrastructure;

import com.han.commerce.stock.domain.StockHistory;
import com.han.commerce.stock.domain.StockHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockHistoryJpaRepository extends StockHistoryRepository, JpaRepository<StockHistory, Long> {

}