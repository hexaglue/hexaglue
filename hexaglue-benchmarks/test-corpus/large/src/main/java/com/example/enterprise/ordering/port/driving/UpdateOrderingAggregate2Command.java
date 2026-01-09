package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate2Id;

/**
 * Command to update an existing OrderingAggregate2.
 */
public record UpdateOrderingAggregate2Command(
    OrderingAggregate2Id id,
    String name
) {
    public UpdateOrderingAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
