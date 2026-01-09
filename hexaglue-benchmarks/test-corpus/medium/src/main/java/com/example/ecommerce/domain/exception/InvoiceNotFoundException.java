package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.InvoiceId;

/**
 * Exception thrown when Invoice is not found.
 */
public class InvoiceNotFoundException extends DomainException {
    private final InvoiceId id;

    public InvoiceNotFoundException(InvoiceId id) {
        super("Invoice not found: " + id);
        this.id = id;
    }

    public InvoiceId getId() {
        return id;
    }
}
