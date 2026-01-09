package com.example.enterprise.warehouse.domain.model;

/**
 * Value Object representing a description in warehouse context.
 */
public record WarehouseDescription(String value) {
    public WarehouseDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static WarehouseDescription empty() {
        return new WarehouseDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
