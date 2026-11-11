package com.han.commerce.product.domain;

import com.han.commerce.common.domain.BaseEntity;
import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import com.han.commerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    protected Product() {
    }

    public static Product create(String name, Money price) {
        Product product = new Product();
        product.name = name;
        product.price = price;
        product.status = ProductStatus.ON_SALE;
        return product;
    }

    public void validateOnSale() {
        if (status != ProductStatus.ON_SALE) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_ON_SALE, name);
        }
    }

    public void stopSale() {
        this.status = ProductStatus.STOPPED;
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

    public ProductStatus getStatus() {
        return status;
    }

}