package com.example.enterprise.catalog.port.driving;

import java.util.List;

/**
 * Command to create a new CatalogAggregate1.
 */
public record CreateCatalogAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCatalogAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
