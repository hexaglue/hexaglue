package com.example.ecommerce.domain.model;

/**
 * Entity representing A product attribute (size, color, etc).
 */
public class ProductAttribute {
    private final String id;
    private String name;

    public ProductAttribute(String id, String name) {
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
