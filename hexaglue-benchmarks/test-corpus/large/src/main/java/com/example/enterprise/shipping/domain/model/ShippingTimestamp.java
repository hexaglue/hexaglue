package com.example.enterprise.shipping.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in shipping context.
 */
public record ShippingTimestamp(Instant value) {
    public ShippingTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static ShippingTimestamp now() {
        return new ShippingTimestamp(Instant.now());
    }

    public boolean isBefore(ShippingTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(ShippingTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
