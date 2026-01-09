package com.example.enterprise.ordering.port.driving;

import java.util.List;

/**
 * Command to create a new OrderingAggregate3.
 */
public record CreateOrderingAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateOrderingAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
