package com.example.ecommerce.domain.model;

/**
 * Value Object representing a supplier name.
 */
public record SupplierName(String value) {
    public SupplierName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SupplierName cannot be null or blank");
        }
    }
}
