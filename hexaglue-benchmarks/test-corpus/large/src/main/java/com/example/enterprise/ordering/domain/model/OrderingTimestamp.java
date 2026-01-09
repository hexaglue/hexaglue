package com.example.enterprise.ordering.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in ordering context.
 */
public record OrderingTimestamp(Instant value) {
    public OrderingTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static OrderingTimestamp now() {
        return new OrderingTimestamp(Instant.now());
    }

    public boolean isBefore(OrderingTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(OrderingTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
