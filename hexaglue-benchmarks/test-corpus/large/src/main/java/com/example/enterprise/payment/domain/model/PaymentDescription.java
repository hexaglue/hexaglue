package com.example.enterprise.payment.domain.model;

/**
 * Value Object representing a description in payment context.
 */
public record PaymentDescription(String value) {
    public PaymentDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static PaymentDescription empty() {
        return new PaymentDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
