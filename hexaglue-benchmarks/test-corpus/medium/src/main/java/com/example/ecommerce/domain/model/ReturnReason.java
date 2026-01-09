package com.example.ecommerce.domain.model;

/**
 * Value Object representing a return reason.
 */
public record ReturnReason(String value) {
    public ReturnReason {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReturnReason cannot be null or blank");
        }
    }
}
