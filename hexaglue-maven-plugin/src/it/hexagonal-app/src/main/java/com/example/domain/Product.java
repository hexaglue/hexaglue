package com.example.domain;

import java.math.BigDecimal;

/**
 * Product aggregate root.
 */
public class Product {

    private final ProductId id;
    private String name;
    private BigDecimal price;

    public Product(ProductId id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void updatePrice(BigDecimal newPrice) {
        this.price = newPrice;
    }
}
