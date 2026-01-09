package com.example.enterprise.warehouse.port.driven;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for WarehouseAggregate2 persistence.
 */
public interface WarehouseAggregate2Repository {
    WarehouseAggregate2 save(WarehouseAggregate2 entity);
    Optional<WarehouseAggregate2> findById(WarehouseAggregate2Id id);
    List<WarehouseAggregate2> findAll();
    void deleteById(WarehouseAggregate2Id id);
    boolean existsById(WarehouseAggregate2Id id);
    long count();
}
