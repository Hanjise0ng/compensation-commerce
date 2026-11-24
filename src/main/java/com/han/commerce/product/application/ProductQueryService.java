package com.han.commerce.product.application;

import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.product.application.dto.ProductInfo;
import com.han.commerce.product.domain.Product;
import com.han.commerce.product.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductInfo> getOnSaleProducts(Collection<Long> productIds) {
        Map<Long, Product> found = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            found.put(product.getId(), product);
        }

        Map<Long, ProductInfo> products = new HashMap<>();
        for (Long productId : productIds) {
            Product product = found.get(productId);
            if (product == null) {
                throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "상품 " + productId);
            }
            product.validateOnSale();
            products.put(productId, ProductInfo.from(product));
        }
        return products;
    }

}