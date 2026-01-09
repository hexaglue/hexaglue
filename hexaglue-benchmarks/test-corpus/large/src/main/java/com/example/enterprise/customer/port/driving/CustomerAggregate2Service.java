package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate2;
import com.example.enterprise.customer.domain.model.CustomerAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for CustomerAggregate2 operations.
 */
public interface CustomerAggregate2Service {
    CustomerAggregate2Id create(CreateCustomerAggregate2Command command);
    CustomerAggregate2 get(CustomerAggregate2Id id);
    List<CustomerAggregate2> list();
    void update(UpdateCustomerAggregate2Command command);
    void delete(CustomerAggregate2Id id);
    void activate(CustomerAggregate2Id id);
    void complete(CustomerAggregate2Id id);
}
