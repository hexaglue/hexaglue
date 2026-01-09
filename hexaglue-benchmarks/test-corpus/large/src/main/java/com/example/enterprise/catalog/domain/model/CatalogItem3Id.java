package com.example.enterprise.catalog.domain.model;

import java.util.UUID;

public record CatalogItem3Id(UUID value) {
    public CatalogItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static CatalogItem3Id generate() {
        return new CatalogItem3Id(UUID.randomUUID());
    }
}
