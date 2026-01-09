package com.example.ecommerce.domain.model;

/**
 * Value Object representing Shipping method (standard, express, etc).
 */
public record ShippingMethod(String value) {
    public ShippingMethod {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShippingMethod cannot be null or blank");
        }
    }
}
