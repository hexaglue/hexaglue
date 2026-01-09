package com.example.ecommerce.domain.model;

/**
 * Value Object representing Refund amount.
 */
public record RefundAmount(String value) {
    public RefundAmount {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RefundAmount cannot be null or blank");
        }
    }
}
