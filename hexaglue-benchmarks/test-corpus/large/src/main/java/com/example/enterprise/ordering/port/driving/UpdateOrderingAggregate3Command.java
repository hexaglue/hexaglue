package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate3Id;

/**
 * Command to update an existing OrderingAggregate3.
 */
public record UpdateOrderingAggregate3Command(
    OrderingAggregate3Id id,
    String name
) {
    public UpdateOrderingAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
