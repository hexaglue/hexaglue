package com.example.enterprise.catalog.domain.exception;

/**
 * Exception thrown when validation fails in catalog context.
 */
public class CatalogValidationException extends CatalogDomainException {
    public CatalogValidationException(String message) {
        super(message);
    }
}
