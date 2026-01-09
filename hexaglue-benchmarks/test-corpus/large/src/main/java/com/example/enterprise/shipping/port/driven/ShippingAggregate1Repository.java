package com.example.enterprise.shipping.port.driven;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1;
import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for ShippingAggregate1 persistence.
 */
public interface ShippingAggregate1Repository {
    ShippingAggregate1 save(ShippingAggregate1 entity);
    Optional<ShippingAggregate1> findById(ShippingAggregate1Id id);
    List<ShippingAggregate1> findAll();
    void deleteById(ShippingAggregate1Id id);
    boolean existsById(ShippingAggregate1Id id);
    long count();
}
