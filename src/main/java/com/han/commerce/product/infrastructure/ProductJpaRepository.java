package com.han.commerce.product.infrastructure;

import com.han.commerce.product.domain.Product;
import com.han.commerce.product.domain.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends ProductRepository, JpaRepository<Product, Long> {

}