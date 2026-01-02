package com.ecommerce.domain.product;

import com.ecommerce.domain.order.Money;

/**
 * Product aggregate root.
 */
public class Product {

    private final ProductId id;
    private String name;
    private String description;
    private Money price;
    private int stockQuantity;
    private boolean active;

    public Product(ProductId id, String name, String description, Money price, int stockQuantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = true;
    }

    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void updateDetails(String name, String description, Money price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public void adjustStock(int quantity) {
        this.stockQuantity += quantity;
        if (this.stockQuantity < 0) {
            throw new IllegalStateException("Stock cannot be negative");
        }
    }

    public boolean hasStock(int quantity) {
        return this.stockQuantity >= quantity;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
