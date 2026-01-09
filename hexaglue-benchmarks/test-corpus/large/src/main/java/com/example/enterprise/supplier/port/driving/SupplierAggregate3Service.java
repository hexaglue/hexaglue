package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate3;
import com.example.enterprise.supplier.domain.model.SupplierAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for SupplierAggregate3 operations.
 */
public interface SupplierAggregate3Service {
    SupplierAggregate3Id create(CreateSupplierAggregate3Command command);
    SupplierAggregate3 get(SupplierAggregate3Id id);
    List<SupplierAggregate3> list();
    void update(UpdateSupplierAggregate3Command command);
    void delete(SupplierAggregate3Id id);
    void activate(SupplierAggregate3Id id);
    void complete(SupplierAggregate3Id id);
}
