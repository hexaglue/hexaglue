package com.example.enterprise.customer.port.driving;

import com.example.enterprise.customer.domain.model.CustomerAggregate3;
import com.example.enterprise.customer.domain.model.CustomerAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for CustomerAggregate3 operations.
 */
public interface CustomerAggregate3Service {
    CustomerAggregate3Id create(CreateCustomerAggregate3Command command);
    CustomerAggregate3 get(CustomerAggregate3Id id);
    List<CustomerAggregate3> list();
    void update(UpdateCustomerAggregate3Command command);
    void delete(CustomerAggregate3Id id);
    void activate(CustomerAggregate3Id id);
    void complete(CustomerAggregate3Id id);
}
