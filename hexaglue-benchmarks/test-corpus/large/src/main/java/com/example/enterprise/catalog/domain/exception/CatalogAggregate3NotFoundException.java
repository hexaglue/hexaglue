package com.example.enterprise.catalog.domain.exception;

import com.example.enterprise.catalog.domain.model.CatalogAggregate3Id;

/**
 * Exception thrown when a CatalogAggregate3 is not found.
 */
public class CatalogAggregate3NotFoundException extends CatalogDomainException {
    public CatalogAggregate3NotFoundException(CatalogAggregate3Id id) {
        super("CatalogAggregate3 not found with id: " + id.value());
    }
}
