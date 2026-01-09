package com.example.enterprise.payment.domain.model;

import java.util.UUID;

/**
 * Value Object representing a PaymentAggregate3 identifier.
 */
public record PaymentAggregate3Id(UUID value) {
    public PaymentAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("PaymentAggregate3Id cannot be null");
        }
    }

    public static PaymentAggregate3Id generate() {
        return new PaymentAggregate3Id(UUID.randomUUID());
    }

    public static PaymentAggregate3Id of(String value) {
        return new PaymentAggregate3Id(UUID.fromString(value));
    }
}
