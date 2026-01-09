package com.example.enterprise.inventory.port.driven;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1;
import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for InventoryAggregate1 persistence.
 */
public interface InventoryAggregate1Repository {
    InventoryAggregate1 save(InventoryAggregate1 entity);
    Optional<InventoryAggregate1> findById(InventoryAggregate1Id id);
    List<InventoryAggregate1> findAll();
    void deleteById(InventoryAggregate1Id id);
    boolean existsById(InventoryAggregate1Id id);
    long count();
}
