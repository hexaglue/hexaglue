package com.example.ecommerce.domain.model;

/**
 * Entity representing A shipping label.
 */
public class ShippingLabel {
    private final String id;
    private String name;

    public ShippingLabel(String id, String name) {
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
