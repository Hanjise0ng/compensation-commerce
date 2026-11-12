package com.han.commerce.product.domain;

import java.util.List;

public interface ProductRepository {

    List<Product> findAllById(Iterable<Long> ids);

}