package com.example.enterprise.catalog.port.driving;

import java.util.List;

/**
 * Command to create a new CatalogAggregate3.
 */
public record CreateCatalogAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateCatalogAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
