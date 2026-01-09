package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

public record CatalogItem2Id(UUID value) {
    public CatalogItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CatalogItem2Id generate() {
        return new CatalogItem2Id(UUID.randomUUID());
    }
}
