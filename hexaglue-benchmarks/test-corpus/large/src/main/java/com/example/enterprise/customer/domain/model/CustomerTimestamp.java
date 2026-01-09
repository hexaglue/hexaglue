package com.example.enterprise.customer.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in customer context.
 */
public record CustomerTimestamp(Instant value) {
    public CustomerTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static CustomerTimestamp now() {
        return new CustomerTimestamp(Instant.now());
    }

    public boolean isBefore(CustomerTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(CustomerTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
