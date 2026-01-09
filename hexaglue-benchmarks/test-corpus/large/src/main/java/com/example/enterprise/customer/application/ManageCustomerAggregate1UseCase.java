package com.example.enterprise.customer.application;

import com.example.enterprise.customer.domain.model.CustomerAggregate1;
import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;
import com.example.enterprise.customer.port.driven.CustomerAggregate1Repository;
import com.example.enterprise.customer.port.driving.CustomerAggregate1Service;
import com.example.enterprise.customer.port.driving.CreateCustomerAggregate1Command;
import com.example.enterprise.customer.port.driving.UpdateCustomerAggregate1Command;
import com.example.enterprise.customer.domain.exception.CustomerAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CustomerAggregate1.
 */
public class ManageCustomerAggregate1UseCase implements CustomerAggregate1Service {
    private final CustomerAggregate1Repository repository;

    public ManageCustomerAggregate1UseCase(CustomerAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerAggregate1Id create(CreateCustomerAggregate1Command command) {
        CustomerAggregate1Id id = CustomerAggregate1Id.generate();
        CustomerAggregate1 entity = new CustomerAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CustomerAggregate1 get(CustomerAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CustomerAggregate1NotFoundException(id));
    }

    @Override
    public List<CustomerAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCustomerAggregate1Command command) {
        CustomerAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CustomerAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new CustomerAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CustomerAggregate1Id id) {
        CustomerAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CustomerAggregate1Id id) {
        CustomerAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
