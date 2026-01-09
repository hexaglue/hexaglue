package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

/**
 * Value Object representing a MarketingAggregate2 identifier.
 */
public record MarketingAggregate2Id(UUID value) {
    public MarketingAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("MarketingAggregate2Id cannot be null");
        }
    }

    public static MarketingAggregate2Id generate() {
        return new MarketingAggregate2Id(UUID.randomUUID());
    }

    public static MarketingAggregate2Id of(String value) {
        return new MarketingAggregate2Id(UUID.fromString(value));
    }
}
