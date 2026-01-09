package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;

/**
 * Command to update an existing CatalogAggregate1.
 */
public record UpdateCatalogAggregate1Command(
    CatalogAggregate1Id id,
    String name
) {
    public UpdateCatalogAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
