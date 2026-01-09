package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;

/**
 * Command to update an existing OrderingAggregate1.
 */
public record UpdateOrderingAggregate1Command(
    OrderingAggregate1Id id,
    String name
) {
    public UpdateOrderingAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
