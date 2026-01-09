package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate2Id;

/**
 * Command to update an existing CustomerAggregate2.
 */
public record UpdateCustomerAggregate2Command(
    CustomerAggregate2Id id,
    String name
) {
    public UpdateCustomerAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
