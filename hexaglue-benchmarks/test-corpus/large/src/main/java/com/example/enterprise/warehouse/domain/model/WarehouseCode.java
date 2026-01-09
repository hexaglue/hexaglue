package com.example.enterprise.warehouse.domain.model;

/**
 * Value Object representing a code in warehouse context.
 */
public record WarehouseCode(String value) {
    public WarehouseCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
