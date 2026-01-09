package com.example.enterprise.customer.port.driven;

import com.example.enterprise.customer.domain.model.CustomerAggregate1;
import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CustomerAggregate1 persistence.
 */
public interface CustomerAggregate1Repository {
    CustomerAggregate1 save(CustomerAggregate1 entity);
    Optional<CustomerAggregate1> findById(CustomerAggregate1Id id);
    List<CustomerAggregate1> findAll();
    void deleteById(CustomerAggregate1Id id);
    boolean existsById(CustomerAggregate1Id id);
    long count();
}
