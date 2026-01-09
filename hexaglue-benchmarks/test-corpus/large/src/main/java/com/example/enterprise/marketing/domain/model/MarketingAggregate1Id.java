package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

/**
 * Value Object representing a MarketingAggregate1 identifier.
 */
public record MarketingAggregate1Id(UUID value) {
    public MarketingAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("MarketingAggregate1Id cannot be null");
        }
    }

    public static MarketingAggregate1Id generate() {
        return new MarketingAggregate1Id(UUID.randomUUID());
    }

    public static MarketingAggregate1Id of(String value) {
        return new MarketingAggregate1Id(UUID.fromString(value));
    }
}
