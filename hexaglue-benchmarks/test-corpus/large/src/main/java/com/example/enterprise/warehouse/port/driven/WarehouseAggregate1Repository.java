package com.example.enterprise.warehouse.port.driven;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for WarehouseAggregate1 persistence.
 */
public interface WarehouseAggregate1Repository {
    WarehouseAggregate1 save(WarehouseAggregate1 entity);
    Optional<WarehouseAggregate1> findById(WarehouseAggregate1Id id);
    List<WarehouseAggregate1> findAll();
    void deleteById(WarehouseAggregate1Id id);
    boolean existsById(WarehouseAggregate1Id id);
    long count();
}
