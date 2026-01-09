package com.example.enterprise.ordering.port.driving;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1;
import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for OrderingAggregate1 operations.
 */
public interface OrderingAggregate1Service {
    OrderingAggregate1Id create(CreateOrderingAggregate1Command command);
    OrderingAggregate1 get(OrderingAggregate1Id id);
    List<OrderingAggregate1> list();
    void update(UpdateOrderingAggregate1Command command);
    void delete(OrderingAggregate1Id id);
    void activate(OrderingAggregate1Id id);
    void complete(OrderingAggregate1Id id);
}
