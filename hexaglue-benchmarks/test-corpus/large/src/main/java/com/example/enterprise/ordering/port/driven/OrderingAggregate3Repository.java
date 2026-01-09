package com.example.enterprise.ordering.port.driven;

import com.example.enterprise.ordering.domain.model.OrderingAggregate3;
import com.example.enterprise.ordering.domain.model.OrderingAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for OrderingAggregate3 persistence.
 */
public interface OrderingAggregate3Repository {
    OrderingAggregate3 save(OrderingAggregate3 entity);
    Optional<OrderingAggregate3> findById(OrderingAggregate3Id id);
    List<OrderingAggregate3> findAll();
    void deleteById(OrderingAggregate3Id id);
    boolean existsById(OrderingAggregate3Id id);
    long count();
}
