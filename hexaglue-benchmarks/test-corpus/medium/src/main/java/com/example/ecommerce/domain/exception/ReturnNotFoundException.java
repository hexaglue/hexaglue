package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.ReturnId;

/**
 * Exception thrown when Return is not found.
 */
public class ReturnNotFoundException extends DomainException {
    private final ReturnId id;

    public ReturnNotFoundException(ReturnId id) {
        super("Return not found: " + id);
        this.id = id;
    }

    public ReturnId getId() {
        return id;
    }
}
