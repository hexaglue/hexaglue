package com.example.enterprise.customer.domain.exception;

import com.example.enterprise.customer.domain.model.CustomerAggregate3Id;

/**
 * Exception thrown when a CustomerAggregate3 is not found.
 */
public class CustomerAggregate3NotFoundException extends CustomerDomainException {
    public CustomerAggregate3NotFoundException(CustomerAggregate3Id id) {
        super("CustomerAggregate3 not found with id: " + id.value());
    }
}
