package com.example.enterprise.shipping.port.driven;

import com.example.enterprise.shipping.domain.model.ShippingAggregate3;
import com.example.enterprise.shipping.domain.model.ShippingAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for ShippingAggregate3 persistence.
 */
public interface ShippingAggregate3Repository {
    ShippingAggregate3 save(ShippingAggregate3 entity);
    Optional<ShippingAggregate3> findById(ShippingAggregate3Id id);
    List<ShippingAggregate3> findAll();
    void deleteById(ShippingAggregate3Id id);
    boolean existsById(ShippingAggregate3Id id);
    long count();
}
