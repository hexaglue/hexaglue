package com.example.enterprise.catalog.port.driving;

import com.example.enterprise.catalog.domain.model.CatalogAggregate3Id;

/**
 * Command to update an existing CatalogAggregate3.
 */
public record UpdateCatalogAggregate3Command(
    CatalogAggregate3Id id,
    String name
) {
    public UpdateCatalogAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
