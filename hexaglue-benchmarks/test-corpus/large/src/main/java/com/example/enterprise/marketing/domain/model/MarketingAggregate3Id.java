package com.example.enterprise.marketing.domain.model;

import java.util.UUID;

/**
 * Value Object representing a MarketingAggregate3 identifier.
 */
public record MarketingAggregate3Id(UUID value) {
    public MarketingAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("MarketingAggregate3Id cannot be null");
        }
    }

    public static MarketingAggregate3Id generate() {
        return new MarketingAggregate3Id(UUID.randomUUID());
    }

    public static MarketingAggregate3Id of(String value) {
        return new MarketingAggregate3Id(UUID.fromString(value));
    }
}
