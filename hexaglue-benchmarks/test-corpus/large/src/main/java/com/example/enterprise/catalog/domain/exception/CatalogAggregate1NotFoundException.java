package com.example.enterprise.catalog.domain.exception;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;

/**
 * Exception thrown when a CatalogAggregate1 is not found.
 */
public class CatalogAggregate1NotFoundException extends CatalogDomainException {
    public CatalogAggregate1NotFoundException(CatalogAggregate1Id id) {
        super("CatalogAggregate1 not found with id: " + id.value());
    }
}
