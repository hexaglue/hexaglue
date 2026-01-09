package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate2;
import com.example.enterprise.supplier.domain.model.SupplierAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for SupplierAggregate2 operations.
 */
public interface SupplierAggregate2Service {
    SupplierAggregate2Id create(CreateSupplierAggregate2Command command);
    SupplierAggregate2 get(SupplierAggregate2Id id);
    List<SupplierAggregate2> list();
    void update(UpdateSupplierAggregate2Command command);
    void delete(SupplierAggregate2Id id);
    void activate(SupplierAggregate2Id id);
    void complete(SupplierAggregate2Id id);
}
