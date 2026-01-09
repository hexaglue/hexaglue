package com.example.enterprise.warehouse.application;

import com.example.enterprise.warehouse.domain.model.*;
import com.example.enterprise.warehouse.port.driven.WarehouseAggregate1Repository;
import com.example.enterprise.warehouse.domain.specification.WarehouseSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for warehouse read operations.
 */
public class WarehouseQueryService {
    private final WarehouseAggregate1Repository repository;

    public WarehouseQueryService(WarehouseAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<WarehouseAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(WarehouseSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<WarehouseAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(WarehouseSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<WarehouseAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(WarehouseSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(WarehouseSpecifications.isActive())
            .count();
    }
}
