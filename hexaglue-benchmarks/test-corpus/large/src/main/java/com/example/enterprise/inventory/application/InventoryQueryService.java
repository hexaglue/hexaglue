package com.example.enterprise.inventory.application;

import com.example.enterprise.inventory.domain.model.*;
import com.example.enterprise.inventory.port.driven.InventoryAggregate1Repository;
import com.example.enterprise.inventory.domain.specification.InventorySpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for inventory read operations.
 */
public class InventoryQueryService {
    private final InventoryAggregate1Repository repository;

    public InventoryQueryService(InventoryAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<InventoryAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(InventorySpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<InventoryAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(InventorySpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<InventoryAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(InventorySpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(InventorySpecifications.isActive())
            .count();
    }
}
