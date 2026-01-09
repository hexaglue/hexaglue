package com.example.enterprise.supplier.port.driven;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1;
import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for SupplierAggregate1 persistence.
 */
public interface SupplierAggregate1Repository {
    SupplierAggregate1 save(SupplierAggregate1 entity);
    Optional<SupplierAggregate1> findById(SupplierAggregate1Id id);
    List<SupplierAggregate1> findAll();
    void deleteById(SupplierAggregate1Id id);
    boolean existsById(SupplierAggregate1Id id);
    long count();
}
