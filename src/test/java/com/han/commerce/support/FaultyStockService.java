package com.han.commerce.support;

import com.han.commerce.stock.application.StockService;
import com.han.commerce.stock.domain.StockHistoryRepository;
import com.han.commerce.stock.domain.StockRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Primary
public class FaultyStockService extends StockService {

    private final AtomicBoolean failNext = new AtomicBoolean();

    public FaultyStockService(StockRepository stockRepository, StockHistoryRepository stockHistoryRepository) {
        super(stockRepository, stockHistoryRepository);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void failNextCommit() {
        failNext.set(true);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void reset() {
        failNext.set(false);
    }

    @Override
    public void commit(Long orderId, Map<Long, Integer> quantityByProductId) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("DB 연결이 끊어졌습니다 (테스트용 오류)");
        }
        super.commit(orderId, quantityByProductId);
    }

}