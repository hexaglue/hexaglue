package com.example.ecommerce.domain.model;

/**
 * Entity representing An item being returned.
 */
public class ReturnItem {
    private final String id;
    private String name;

    public ReturnItem(String id, String name) {
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
