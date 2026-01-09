package com.example.enterprise.shipping.port.driving;

import com.example.enterprise.shipping.domain.model.ShippingAggregate3;
import com.example.enterprise.shipping.domain.model.ShippingAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for ShippingAggregate3 operations.
 */
public interface ShippingAggregate3Service {
    ShippingAggregate3Id create(CreateShippingAggregate3Command command);
    ShippingAggregate3 get(ShippingAggregate3Id id);
    List<ShippingAggregate3> list();
    void update(UpdateShippingAggregate3Command command);
    void delete(ShippingAggregate3Id id);
    void activate(ShippingAggregate3Id id);
    void complete(ShippingAggregate3Id id);
}
