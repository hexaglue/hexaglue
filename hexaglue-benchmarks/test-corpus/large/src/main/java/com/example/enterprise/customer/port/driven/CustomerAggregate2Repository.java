package com.example.enterprise.customer.port.driven;

import com.example.enterprise.customer.domain.model.CustomerAggregate2;
import com.example.enterprise.customer.domain.model.CustomerAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CustomerAggregate2 persistence.
 */
public interface CustomerAggregate2Repository {
    CustomerAggregate2 save(CustomerAggregate2 entity);
    Optional<CustomerAggregate2> findById(CustomerAggregate2Id id);
    List<CustomerAggregate2> findAll();
    void deleteById(CustomerAggregate2Id id);
    boolean existsById(CustomerAggregate2Id id);
    long count();
}
