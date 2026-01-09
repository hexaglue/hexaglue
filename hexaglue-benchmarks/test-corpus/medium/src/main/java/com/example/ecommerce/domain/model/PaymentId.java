package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a Payment identifier.
 */
public record PaymentId(UUID value) {
    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId cannot be null");
        }
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId of(String value) {
        return new PaymentId(UUID.fromString(value));
    }
}
