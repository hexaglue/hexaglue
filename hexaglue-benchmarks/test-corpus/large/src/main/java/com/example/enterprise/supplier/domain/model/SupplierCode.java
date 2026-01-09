package com.example.enterprise.supplier.domain.model;

/**
 * Value Object representing a code in supplier context.
 */
public record SupplierCode(String value) {
    public SupplierCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
