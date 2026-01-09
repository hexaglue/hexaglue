package com.example.enterprise.inventory.port.driven;

import com.example.enterprise.inventory.domain.model.InventoryAggregate2;
import com.example.enterprise.inventory.domain.model.InventoryAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for InventoryAggregate2 persistence.
 */
public interface InventoryAggregate2Repository {
    InventoryAggregate2 save(InventoryAggregate2 entity);
    Optional<InventoryAggregate2> findById(InventoryAggregate2Id id);
    List<InventoryAggregate2> findAll();
    void deleteById(InventoryAggregate2Id id);
    boolean existsById(InventoryAggregate2Id id);
    long count();
}
