package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CatalogAggregate2 identifier.
 */
public record CatalogAggregate2Id(UUID value) {
    public CatalogAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("CatalogAggregate2Id cannot be null");
        }
    }

    public static CatalogAggregate2Id generate() {
        return new CatalogAggregate2Id(UUID.randomUUID());
    }

    public static CatalogAggregate2Id of(String value) {
        return new CatalogAggregate2Id(UUID.fromString(value));
    }
}
