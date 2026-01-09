package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.Email;

/**
 * Exception thrown when attempting to register a customer with duplicate email.
 */
public class DuplicateCustomerException extends DomainException {
    private final Email email;

    public DuplicateCustomerException(Email email) {
        super("Customer already exists with email: " + email.value());
        this.email = email;
    }

    public Email getEmail() {
        return email;
    }
}
