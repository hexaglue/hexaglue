package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1;
import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for SupplierAggregate1 operations.
 */
public interface SupplierAggregate1Service {
    SupplierAggregate1Id create(CreateSupplierAggregate1Command command);
    SupplierAggregate1 get(SupplierAggregate1Id id);
    List<SupplierAggregate1> list();
    void update(UpdateSupplierAggregate1Command command);
    void delete(SupplierAggregate1Id id);
    void activate(SupplierAggregate1Id id);
    void complete(SupplierAggregate1Id id);
}
