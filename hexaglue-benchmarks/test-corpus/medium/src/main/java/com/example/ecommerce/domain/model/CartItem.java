package com.example.ecommerce.domain.model;

/**
 * Entity representing A line item in a shopping cart.
 */
public class CartItem {
    private final String id;
    private String name;

    public CartItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
