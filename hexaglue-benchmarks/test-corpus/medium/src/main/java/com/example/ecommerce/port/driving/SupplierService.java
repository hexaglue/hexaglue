package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.SupplierId;
import com.example.ecommerce.domain.model.Supplier;
import java.util.List;

/**
 * Driving port (primary) for supplier operations.
 */
public interface SupplierService {
    SupplierId create(CreateSupplierCommand command);

    Supplier getSupplier(SupplierId id);

    List<Supplier> getAll();

    void update(SupplierId id, UpdateSupplierCommand command);

    void delete(SupplierId id);
}
