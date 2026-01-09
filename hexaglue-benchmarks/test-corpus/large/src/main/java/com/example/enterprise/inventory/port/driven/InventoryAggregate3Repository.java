package com.example.enterprise.inventory.port.driven;

import com.example.enterprise.inventory.domain.model.InventoryAggregate3;
import com.example.enterprise.inventory.domain.model.InventoryAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for InventoryAggregate3 persistence.
 */
public interface InventoryAggregate3Repository {
    InventoryAggregate3 save(InventoryAggregate3 entity);
    Optional<InventoryAggregate3> findById(InventoryAggregate3Id id);
    List<InventoryAggregate3> findAll();
    void deleteById(InventoryAggregate3Id id);
    boolean existsById(InventoryAggregate3Id id);
    long count();
}
