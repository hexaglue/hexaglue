package com.example.enterprise.customer.port.driven;

import com.example.enterprise.customer.domain.model.CustomerAggregate3;
import com.example.enterprise.customer.domain.model.CustomerAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for CustomerAggregate3 persistence.
 */
public interface CustomerAggregate3Repository {
    CustomerAggregate3 save(CustomerAggregate3 entity);
    Optional<CustomerAggregate3> findById(CustomerAggregate3Id id);
    List<CustomerAggregate3> findAll();
    void deleteById(CustomerAggregate3Id id);
    boolean existsById(CustomerAggregate3Id id);
    long count();
}
