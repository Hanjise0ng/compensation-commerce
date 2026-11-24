package com.han.commerce.product.application.dto;

import com.han.commerce.common.domain.Money;
import com.han.commerce.product.domain.Product;

public class ProductInfo {

    private final Long id;
    private final String name;
    private final Money price;

    private ProductInfo(Long id, String name, Money price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public static ProductInfo from(Product product) {
        return new ProductInfo(product.getId(), product.getName(), product.getPrice());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

}