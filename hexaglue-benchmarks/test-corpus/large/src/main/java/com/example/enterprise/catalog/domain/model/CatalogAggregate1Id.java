package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CatalogAggregate1 identifier.
 */
public record CatalogAggregate1Id(UUID value) {
    public CatalogAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("CatalogAggregate1Id cannot be null");
        }
    }

    public static CatalogAggregate1Id generate() {
        return new CatalogAggregate1Id(UUID.randomUUID());
    }

    public static CatalogAggregate1Id of(String value) {
        return new CatalogAggregate1Id(UUID.fromString(value));
    }
}
