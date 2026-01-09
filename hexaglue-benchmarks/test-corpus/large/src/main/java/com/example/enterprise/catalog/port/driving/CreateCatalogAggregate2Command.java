package com.example.enterprise.catalog.port.driving;

import java.util.List;

/**
 * Command to create a new CatalogAggregate2.
 */
public record CreateCatalogAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCatalogAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
