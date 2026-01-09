package com.example.enterprise.catalog.domain.exception;

/**
 * Base exception for catalog domain errors.
 */
public class CatalogDomainException extends RuntimeException {
    public CatalogDomainException(String message) {
        super(message);
    }

    public CatalogDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
