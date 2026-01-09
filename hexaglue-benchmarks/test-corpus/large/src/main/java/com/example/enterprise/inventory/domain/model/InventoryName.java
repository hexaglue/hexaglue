package com.example.enterprise.inventory.domain.model;

/**
 * Value Object representing a name in inventory context.
 */
public record InventoryName(String value) {
    public InventoryName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
