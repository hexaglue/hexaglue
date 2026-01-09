package com.example.ecommerce.domain.model;

/**
 * Value Object representing a warehouse code.
 */
public record WarehouseCode(String value) {
    public WarehouseCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WarehouseCode cannot be null or blank");
        }
    }
}
