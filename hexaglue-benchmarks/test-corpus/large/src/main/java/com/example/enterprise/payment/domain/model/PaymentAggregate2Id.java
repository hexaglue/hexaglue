package com.example.enterprise.payment.domain.model;

import java.util.UUID;

/**
 * Value Object representing a PaymentAggregate2 identifier.
 */
public record PaymentAggregate2Id(UUID value) {
    public PaymentAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("PaymentAggregate2Id cannot be null");
        }
    }

    public static PaymentAggregate2Id generate() {
        return new PaymentAggregate2Id(UUID.randomUUID());
    }

    public static PaymentAggregate2Id of(String value) {
        return new PaymentAggregate2Id(UUID.fromString(value));
    }
}
