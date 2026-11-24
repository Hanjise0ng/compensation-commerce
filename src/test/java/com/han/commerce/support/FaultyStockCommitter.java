package com.han.commerce.support;

import com.han.commerce.order.application.checkout.OrderStockCommitter;
import com.han.commerce.order.domain.Order;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Primary
public class FaultyStockCommitter extends OrderStockCommitter {

    private final AtomicBoolean failNext = new AtomicBoolean();

    public FaultyStockCommitter(StockRepository stockRepository, StockHistoryRepository stockHistoryRepository) {
        super(stockRepository, stockHistoryRepository);
    }

    public void failNextCommit() {
        failNext.set(true);
    }

    public void reset() {
        failNext.set(false);
    }

    @Override
    public void commit(Order order) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("DB 연결이 끊어졌습니다 (테스트용 오류)");
        }
        super.commit(order);
    }

}