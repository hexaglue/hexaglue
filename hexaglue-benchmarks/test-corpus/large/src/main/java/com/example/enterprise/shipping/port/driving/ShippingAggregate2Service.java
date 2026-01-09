package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate2;
import com.example.enterprise.shipping.domain.model.ShippingAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for ShippingAggregate2 operations.
 */
public interface ShippingAggregate2Service {
    ShippingAggregate2Id create(CreateShippingAggregate2Command command);
    ShippingAggregate2 get(ShippingAggregate2Id id);
    List<ShippingAggregate2> list();
    void update(UpdateShippingAggregate2Command command);
    void delete(ShippingAggregate2Id id);
    void activate(ShippingAggregate2Id id);
    void complete(ShippingAggregate2Id id);
}
