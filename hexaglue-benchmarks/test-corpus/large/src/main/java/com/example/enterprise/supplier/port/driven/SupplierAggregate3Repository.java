package com.example.enterprise.supplier.port.driven;

import com.example.enterprise.supplier.domain.model.SupplierAggregate3;
import com.example.enterprise.supplier.domain.model.SupplierAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for SupplierAggregate3 persistence.
 */
public interface SupplierAggregate3Repository {
    SupplierAggregate3 save(SupplierAggregate3 entity);
    Optional<SupplierAggregate3> findById(SupplierAggregate3Id id);
    List<SupplierAggregate3> findAll();
    void deleteById(SupplierAggregate3Id id);
    boolean existsById(SupplierAggregate3Id id);
    long count();
}
