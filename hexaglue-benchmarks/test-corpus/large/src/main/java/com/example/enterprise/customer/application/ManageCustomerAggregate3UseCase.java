package com.example.enterprise.customer.application;

import com.example.enterprise.customer.domain.model.CustomerAggregate3;
import com.example.enterprise.customer.domain.model.CustomerAggregate3Id;
import com.example.enterprise.customer.port.driven.CustomerAggregate3Repository;
import com.example.enterprise.customer.port.driving.CustomerAggregate3Service;
import com.example.enterprise.customer.port.driving.CreateCustomerAggregate3Command;
import com.example.enterprise.customer.port.driving.UpdateCustomerAggregate3Command;
import com.example.enterprise.customer.domain.exception.CustomerAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CustomerAggregate3.
 */
public class ManageCustomerAggregate3UseCase implements CustomerAggregate3Service {
    private final CustomerAggregate3Repository repository;

    public ManageCustomerAggregate3UseCase(CustomerAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerAggregate3Id create(CreateCustomerAggregate3Command command) {
        CustomerAggregate3Id id = CustomerAggregate3Id.generate();
        CustomerAggregate3 entity = new CustomerAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CustomerAggregate3 get(CustomerAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CustomerAggregate3NotFoundException(id));
    }

    @Override
    public List<CustomerAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCustomerAggregate3Command command) {
        CustomerAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CustomerAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new CustomerAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CustomerAggregate3Id id) {
        CustomerAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CustomerAggregate3Id id) {
        CustomerAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
