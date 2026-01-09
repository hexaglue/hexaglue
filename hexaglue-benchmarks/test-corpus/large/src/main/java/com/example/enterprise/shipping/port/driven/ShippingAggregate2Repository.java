package com.example.enterprise.shipping.port.driven;

import com.example.enterprise.shipping.domain.model.ShippingAggregate2;
import com.example.enterprise.shipping.domain.model.ShippingAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for ShippingAggregate2 persistence.
 */
public interface ShippingAggregate2Repository {
    ShippingAggregate2 save(ShippingAggregate2 entity);
    Optional<ShippingAggregate2> findById(ShippingAggregate2Id id);
    List<ShippingAggregate2> findAll();
    void deleteById(ShippingAggregate2Id id);
    boolean existsById(ShippingAggregate2Id id);
    long count();
}
