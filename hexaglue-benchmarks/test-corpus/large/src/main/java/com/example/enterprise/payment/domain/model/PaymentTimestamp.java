package com.example.enterprise.payment.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in payment context.
 */
public record PaymentTimestamp(Instant value) {
    public PaymentTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static PaymentTimestamp now() {
        return new PaymentTimestamp(Instant.now());
    }

    public boolean isBefore(PaymentTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(PaymentTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
