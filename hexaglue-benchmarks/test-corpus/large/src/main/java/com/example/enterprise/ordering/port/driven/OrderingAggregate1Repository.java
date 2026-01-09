package com.example.enterprise.ordering.port.driven;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1;
import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for OrderingAggregate1 persistence.
 */
public interface OrderingAggregate1Repository {
    OrderingAggregate1 save(OrderingAggregate1 entity);
    Optional<OrderingAggregate1> findById(OrderingAggregate1Id id);
    List<OrderingAggregate1> findAll();
    void deleteById(OrderingAggregate1Id id);
    boolean existsById(OrderingAggregate1Id id);
    long count();
}
