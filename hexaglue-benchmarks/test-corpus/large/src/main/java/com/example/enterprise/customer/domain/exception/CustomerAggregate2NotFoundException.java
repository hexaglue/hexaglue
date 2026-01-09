package com.example.enterprise.customer.domain.exception;

import com.example.enterprise.customer.domain.model.CustomerAggregate2Id;

/**
 * Exception thrown when a CustomerAggregate2 is not found.
 */
public class CustomerAggregate2NotFoundException extends CustomerDomainException {
    public CustomerAggregate2NotFoundException(CustomerAggregate2Id id) {
        super("CustomerAggregate2 not found with id: " + id.value());
    }
}
