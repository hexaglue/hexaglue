package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.CustomerId;

/**
 * Exception thrown when a customer is not found.
 */
public class CustomerNotFoundException extends DomainException {
    private final CustomerId customerId;

    public CustomerNotFoundException(CustomerId customerId) {
        super("Customer not found: " + customerId);
        this.customerId = customerId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
