package com.han.commerce.stock.application;

import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistory;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BiFunction;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StockService {

    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public StockService(StockRepository stockRepository, StockHistoryRepository stockHistoryRepository) {
        this.stockRepository = stockRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    public void reserve(Long orderId, Map<Long, Integer> quantityByProductId) {
        apply(quantityByProductId, (stock, quantity) -> stock.reserve(quantity, orderId));
    }

    public void commit(Long orderId, Map<Long, Integer> quantityByProductId) {
        apply(quantityByProductId, (stock, quantity) -> stock.commit(quantity, orderId));
    }

    public void release(Long orderId, Map<Long, Integer> quantityByProductId) {
        apply(quantityByProductId, (stock, quantity) -> stock.release(quantity, orderId));
    }

    private void apply(Map<Long, Integer> quantityByProductId,
                       BiFunction<Stock, Integer, StockHistory> change) {
        Map<Long, Stock> stocks = lockInProductIdOrder(quantityByProductId.keySet());
        for (Map.Entry<Long, Integer> entry : new TreeMap<>(quantityByProductId).entrySet()) {
            Stock stock = stocks.get(entry.getKey());
            if (stock == null) {
                throw new DomainException(ErrorCode.STOCK_NOT_FOUND, "상품 " + entry.getKey());
            }
            stockHistoryRepository.save(change.apply(stock, entry.getValue()));
        }
    }

    private Map<Long, Stock> lockInProductIdOrder(Collection<Long> productIds) {
        Map<Long, Stock> stocks = new HashMap<>();
        for (Stock stock : stockRepository.findAllByProductIdsForUpdate(new TreeSet<>(productIds))) {
            stocks.put(stock.getProductId(), stock);
        }
        return stocks;
    }

}