package com.example.enterprise.warehouse.domain.model;

/**
 * Value Object representing a name in warehouse context.
 */
public record WarehouseName(String value) {
    public WarehouseName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
