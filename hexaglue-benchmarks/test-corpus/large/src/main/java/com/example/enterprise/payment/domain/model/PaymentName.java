package com.example.enterprise.payment.domain.model;

/**
 * Value Object representing a name in payment context.
 */
public record PaymentName(String value) {
    public PaymentName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
