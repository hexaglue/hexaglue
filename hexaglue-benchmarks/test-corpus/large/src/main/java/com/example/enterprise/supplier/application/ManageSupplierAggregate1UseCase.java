package com.example.enterprise.supplier.application;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1;
import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;
import com.example.enterprise.supplier.port.driven.SupplierAggregate1Repository;
import com.example.enterprise.supplier.port.driving.SupplierAggregate1Service;
import com.example.enterprise.supplier.port.driving.CreateSupplierAggregate1Command;
import com.example.enterprise.supplier.port.driving.UpdateSupplierAggregate1Command;
import com.example.enterprise.supplier.domain.exception.SupplierAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing SupplierAggregate1.
 */
public class ManageSupplierAggregate1UseCase implements SupplierAggregate1Service {
    private final SupplierAggregate1Repository repository;

    public ManageSupplierAggregate1UseCase(SupplierAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public SupplierAggregate1Id create(CreateSupplierAggregate1Command command) {
        SupplierAggregate1Id id = SupplierAggregate1Id.generate();
        SupplierAggregate1 entity = new SupplierAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public SupplierAggregate1 get(SupplierAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new SupplierAggregate1NotFoundException(id));
    }

    @Override
    public List<SupplierAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateSupplierAggregate1Command command) {
        SupplierAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(SupplierAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new SupplierAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(SupplierAggregate1Id id) {
        SupplierAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(SupplierAggregate1Id id) {
        SupplierAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
