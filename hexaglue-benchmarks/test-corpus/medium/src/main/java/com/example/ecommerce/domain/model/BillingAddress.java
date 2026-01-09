package com.example.ecommerce.domain.model;

/**
 * Value Object representing Address for billing purposes.
 */
public record BillingAddress(String value) {
    public BillingAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BillingAddress cannot be null or blank");
        }
    }
}
