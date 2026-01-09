package com.example.enterprise.catalog.application;

import com.example.enterprise.catalog.domain.model.*;
import com.example.enterprise.catalog.port.driven.CatalogAggregate1Repository;
import com.example.enterprise.catalog.domain.specification.CatalogSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for catalog read operations.
 */
public class CatalogQueryService {
    private final CatalogAggregate1Repository repository;

    public CatalogQueryService(CatalogAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<CatalogAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(CatalogSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<CatalogAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(CatalogSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<CatalogAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(CatalogSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(CatalogSpecifications.isActive())
            .count();
    }
}
