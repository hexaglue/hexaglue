package com.example.enterprise.customer.application;

import com.example.enterprise.customer.domain.model.CustomerAggregate2;
import com.example.enterprise.customer.domain.model.CustomerAggregate2Id;
import com.example.enterprise.customer.port.driven.CustomerAggregate2Repository;
import com.example.enterprise.customer.port.driving.CustomerAggregate2Service;
import com.example.enterprise.customer.port.driving.CreateCustomerAggregate2Command;
import com.example.enterprise.customer.port.driving.UpdateCustomerAggregate2Command;
import com.example.enterprise.customer.domain.exception.CustomerAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CustomerAggregate2.
 */
public class ManageCustomerAggregate2UseCase implements CustomerAggregate2Service {
    private final CustomerAggregate2Repository repository;

    public ManageCustomerAggregate2UseCase(CustomerAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerAggregate2Id create(CreateCustomerAggregate2Command command) {
        CustomerAggregate2Id id = CustomerAggregate2Id.generate();
        CustomerAggregate2 entity = new CustomerAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CustomerAggregate2 get(CustomerAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CustomerAggregate2NotFoundException(id));
    }

    @Override
    public List<CustomerAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCustomerAggregate2Command command) {
        CustomerAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CustomerAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new CustomerAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CustomerAggregate2Id id) {
        CustomerAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CustomerAggregate2Id id) {
        CustomerAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
