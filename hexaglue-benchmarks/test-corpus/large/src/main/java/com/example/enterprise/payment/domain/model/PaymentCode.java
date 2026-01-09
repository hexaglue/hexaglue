package com.example.enterprise.payment.domain.model;

/**
 * Value Object representing a code in payment context.
 */
public record PaymentCode(String value) {
    public PaymentCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
