package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate1;
import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for CustomerAggregate1 operations.
 */
public interface CustomerAggregate1Service {
    CustomerAggregate1Id create(CreateCustomerAggregate1Command command);
    CustomerAggregate1 get(CustomerAggregate1Id id);
    List<CustomerAggregate1> list();
    void update(UpdateCustomerAggregate1Command command);
    void delete(CustomerAggregate1Id id);
    void activate(CustomerAggregate1Id id);
    void complete(CustomerAggregate1Id id);
}
