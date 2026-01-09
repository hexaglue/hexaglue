package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Supplier;
import com.example.ecommerce.domain.model.SupplierId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Supplier persistence.
 */
public interface SupplierRepository {
    Supplier save(Supplier entity);

    Optional<Supplier> findById(SupplierId id);

    List<Supplier> findAll();

    void deleteById(SupplierId id);

    boolean existsById(SupplierId id);
}
