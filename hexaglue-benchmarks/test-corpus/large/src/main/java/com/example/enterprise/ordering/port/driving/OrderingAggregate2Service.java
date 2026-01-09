package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate2;
import com.example.enterprise.ordering.domain.model.OrderingAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for OrderingAggregate2 operations.
 */
public interface OrderingAggregate2Service {
    OrderingAggregate2Id create(CreateOrderingAggregate2Command command);
    OrderingAggregate2 get(OrderingAggregate2Id id);
    List<OrderingAggregate2> list();
    void update(UpdateOrderingAggregate2Command command);
    void delete(OrderingAggregate2Id id);
    void activate(OrderingAggregate2Id id);
    void complete(OrderingAggregate2Id id);
}
