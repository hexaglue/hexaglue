package com.example.enterprise.ordering.port.driving;

import java.util.List;

/**
 * Command to create a new OrderingAggregate1.
 */
public record CreateOrderingAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateOrderingAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
