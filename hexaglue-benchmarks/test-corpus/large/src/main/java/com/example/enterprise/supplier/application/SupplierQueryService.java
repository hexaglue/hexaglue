package com.example.enterprise.supplier.application;

import com.example.enterprise.supplier.domain.model.*;
import com.example.enterprise.supplier.port.driven.SupplierAggregate1Repository;
import com.example.enterprise.supplier.domain.specification.SupplierSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for supplier read operations.
 */
public class SupplierQueryService {
    private final SupplierAggregate1Repository repository;

    public SupplierQueryService(SupplierAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<SupplierAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(SupplierSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<SupplierAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(SupplierSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<SupplierAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(SupplierSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(SupplierSpecifications.isActive())
            .count();
    }
}
