package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CatalogAggregate3 identifier.
 */
public record CatalogAggregate3Id(UUID value) {
    public CatalogAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("CatalogAggregate3Id cannot be null");
        }
    }

    public static CatalogAggregate3Id generate() {
        return new CatalogAggregate3Id(UUID.randomUUID());
    }

    public static CatalogAggregate3Id of(String value) {
        return new CatalogAggregate3Id(UUID.fromString(value));
    }
}
