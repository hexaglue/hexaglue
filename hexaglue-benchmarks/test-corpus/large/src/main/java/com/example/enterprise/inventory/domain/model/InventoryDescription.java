package com.example.enterprise.inventory.domain.model;

/**
 * Value Object representing a description in inventory context.
 */
public record InventoryDescription(String value) {
    public InventoryDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static InventoryDescription empty() {
        return new InventoryDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
