package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;

/**
 * Command to update an existing CustomerAggregate1.
 */
public record UpdateCustomerAggregate1Command(
    CustomerAggregate1Id id,
    String name
) {
    public UpdateCustomerAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
