package com.han.commerce.order.application;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.order.domain.Order;
import com.han.commerce.order.domain.OrderItem;
import com.han.commerce.stock.domain.Stock;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

@Component
public class OrderStockCommitter {

    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public OrderStockCommitter(StockRepository stockRepository, StockHistoryRepository stockHistoryRepository) {
        this.stockRepository = stockRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    public void commit(Order order) {
        TreeSet<Long> productIds = new TreeSet<>();
        for (OrderItem item : order.getItems()) {
            productIds.add(item.getProductId());
        }
        Map<Long, Stock> stocks = new HashMap<>();
        for (Stock stock : stockRepository.findAllByProductIdsForUpdate(productIds)) {
            stocks.put(stock.getProductId(), stock);
        }
        for (OrderItem item : order.getItems()) {
            Stock stock = stocks.get(item.getProductId());
            if (stock == null) {
                throw new DomainException(ErrorCode.STOCK_NOT_FOUND, "상품 " + item.getProductId());
            }
            stockHistoryRepository.save(stock.commit(item.getQuantity(), order.getId()));
        }
    }

}