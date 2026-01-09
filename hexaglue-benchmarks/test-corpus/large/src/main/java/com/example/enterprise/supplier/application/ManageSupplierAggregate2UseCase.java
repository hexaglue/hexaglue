package com.example.enterprise.supplier.application;

import com.example.enterprise.supplier.domain.model.SupplierAggregate2;
import com.example.enterprise.supplier.domain.model.SupplierAggregate2Id;
import com.example.enterprise.supplier.port.driven.SupplierAggregate2Repository;
import com.example.enterprise.supplier.port.driving.SupplierAggregate2Service;
import com.example.enterprise.supplier.port.driving.CreateSupplierAggregate2Command;
import com.example.enterprise.supplier.port.driving.UpdateSupplierAggregate2Command;
import com.example.enterprise.supplier.domain.exception.SupplierAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing SupplierAggregate2.
 */
public class ManageSupplierAggregate2UseCase implements SupplierAggregate2Service {
    private final SupplierAggregate2Repository repository;

    public ManageSupplierAggregate2UseCase(SupplierAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public SupplierAggregate2Id create(CreateSupplierAggregate2Command command) {
        SupplierAggregate2Id id = SupplierAggregate2Id.generate();
        SupplierAggregate2 entity = new SupplierAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public SupplierAggregate2 get(SupplierAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new SupplierAggregate2NotFoundException(id));
    }

    @Override
    public List<SupplierAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateSupplierAggregate2Command command) {
        SupplierAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(SupplierAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new SupplierAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(SupplierAggregate2Id id) {
        SupplierAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(SupplierAggregate2Id id) {
        SupplierAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
