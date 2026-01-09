package com.example.enterprise.payment.domain.model;

import java.util.UUID;

/**
 * Value Object representing a PaymentAggregate1 identifier.
 */
public record PaymentAggregate1Id(UUID value) {
    public PaymentAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("PaymentAggregate1Id cannot be null");
        }
    }

    public static PaymentAggregate1Id generate() {
        return new PaymentAggregate1Id(UUID.randomUUID());
    }

    public static PaymentAggregate1Id of(String value) {
        return new PaymentAggregate1Id(UUID.fromString(value));
    }
}
