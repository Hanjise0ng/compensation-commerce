package com.han.commerce.stock.domain;

import java.util.Collection;
import java.util.List;

public interface StockRepository {

    List<Stock> findAllByProductIdsForUpdate(Collection<Long> productIds);

}