package com.example.enterprise.ordering.port.driven;

import com.example.enterprise.ordering.domain.model.OrderingAggregate2;
import com.example.enterprise.ordering.domain.model.OrderingAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for OrderingAggregate2 persistence.
 */
public interface OrderingAggregate2Repository {
    OrderingAggregate2 save(OrderingAggregate2 entity);
    Optional<OrderingAggregate2> findById(OrderingAggregate2Id id);
    List<OrderingAggregate2> findAll();
    void deleteById(OrderingAggregate2Id id);
    boolean existsById(OrderingAggregate2Id id);
    long count();
}
