package com.example.enterprise.marketing.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in marketing context.
 */
public record MarketingTimestamp(Instant value) {
    public MarketingTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static MarketingTimestamp now() {
        return new MarketingTimestamp(Instant.now());
    }

    public boolean isBefore(MarketingTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(MarketingTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
