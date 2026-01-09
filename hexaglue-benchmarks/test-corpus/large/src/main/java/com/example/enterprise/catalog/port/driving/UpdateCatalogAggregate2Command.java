package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate2Id;

/**
 * Command to update an existing CatalogAggregate2.
 */
public record UpdateCatalogAggregate2Command(
    CatalogAggregate2Id id,
    String name
) {
    public UpdateCatalogAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
