package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate3;
import com.example.enterprise.ordering.domain.model.OrderingAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for OrderingAggregate3 operations.
 */
public interface OrderingAggregate3Service {
    OrderingAggregate3Id create(CreateOrderingAggregate3Command command);
    OrderingAggregate3 get(OrderingAggregate3Id id);
    List<OrderingAggregate3> list();
    void update(UpdateOrderingAggregate3Command command);
    void delete(OrderingAggregate3Id id);
    void activate(OrderingAggregate3Id id);
    void complete(OrderingAggregate3Id id);
}
