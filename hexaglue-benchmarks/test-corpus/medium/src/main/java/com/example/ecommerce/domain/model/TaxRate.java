package com.example.ecommerce.domain.model;

/**
 * Value Object representing Tax rate percentage.
 */
public record TaxRate(String value) {
    public TaxRate {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaxRate cannot be null or blank");
        }
    }
}
