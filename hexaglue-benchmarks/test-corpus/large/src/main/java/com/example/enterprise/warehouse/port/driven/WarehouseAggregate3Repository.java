package com.example.enterprise.warehouse.port.driven;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for WarehouseAggregate3 persistence.
 */
public interface WarehouseAggregate3Repository {
    WarehouseAggregate3 save(WarehouseAggregate3 entity);
    Optional<WarehouseAggregate3> findById(WarehouseAggregate3Id id);
    List<WarehouseAggregate3> findAll();
    void deleteById(WarehouseAggregate3Id id);
    boolean existsById(WarehouseAggregate3Id id);
    long count();
}
