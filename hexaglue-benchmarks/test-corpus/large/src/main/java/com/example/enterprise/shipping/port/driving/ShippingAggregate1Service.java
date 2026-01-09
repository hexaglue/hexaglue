package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1;
import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for ShippingAggregate1 operations.
 */
public interface ShippingAggregate1Service {
    ShippingAggregate1Id create(CreateShippingAggregate1Command command);
    ShippingAggregate1 get(ShippingAggregate1Id id);
    List<ShippingAggregate1> list();
    void update(UpdateShippingAggregate1Command command);
    void delete(ShippingAggregate1Id id);
    void activate(ShippingAggregate1Id id);
    void complete(ShippingAggregate1Id id);
}
