package com.example.domain;

/**
 * Product aggregate root.
 * Represents an item that can be ordered.
 */
public class Product {
    private final ProductId id;
    private String name;
    private String description;
    private Money price;

    public Product(ProductId id, String name, String description, Money price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
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

    public void updatePrice(Money newPrice) {
        this.price = newPrice;
    }
}
