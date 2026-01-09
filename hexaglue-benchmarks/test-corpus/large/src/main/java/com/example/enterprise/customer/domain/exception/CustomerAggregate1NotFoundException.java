package com.example.enterprise.customer.domain.exception;

import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;

/**
 * Exception thrown when a CustomerAggregate1 is not found.
 */
public class CustomerAggregate1NotFoundException extends CustomerDomainException {
    public CustomerAggregate1NotFoundException(CustomerAggregate1Id id) {
        super("CustomerAggregate1 not found with id: " + id.value());
    }
}
