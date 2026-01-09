package com.example.enterprise.ordering.port.driving;

import java.util.List;

/**
 * Command to create a new OrderingAggregate2.
 */
public record CreateOrderingAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateOrderingAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
