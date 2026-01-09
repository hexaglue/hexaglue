package com.example.enterprise.inventory.domain.model;

/**
 * Value Object representing a code in inventory context.
 */
public record InventoryCode(String value) {
    public InventoryCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
