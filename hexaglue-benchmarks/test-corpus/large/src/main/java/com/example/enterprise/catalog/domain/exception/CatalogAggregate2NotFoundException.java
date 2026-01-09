package com.example.enterprise.catalog.domain.exception;

import com.example.enterprise.catalog.domain.model.CatalogAggregate2Id;

/**
 * Exception thrown when a CatalogAggregate2 is not found.
 */
public class CatalogAggregate2NotFoundException extends CatalogDomainException {
    public CatalogAggregate2NotFoundException(CatalogAggregate2Id id) {
        super("CatalogAggregate2 not found with id: " + id.value());
    }
}
