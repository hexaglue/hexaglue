package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

public record CatalogItem1Id(UUID value) {
    public CatalogItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CatalogItem1Id generate() {
        return new CatalogItem1Id(UUID.randomUUID());
    }
}
