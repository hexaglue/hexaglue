package com.example.enterprise.supplier.port.driven;

import com.example.enterprise.supplier.domain.model.SupplierAggregate2;
import com.example.enterprise.supplier.domain.model.SupplierAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for SupplierAggregate2 persistence.
 */
public interface SupplierAggregate2Repository {
    SupplierAggregate2 save(SupplierAggregate2 entity);
    Optional<SupplierAggregate2> findById(SupplierAggregate2Id id);
    List<SupplierAggregate2> findAll();
    void deleteById(SupplierAggregate2Id id);
    boolean existsById(SupplierAggregate2Id id);
    long count();
}
