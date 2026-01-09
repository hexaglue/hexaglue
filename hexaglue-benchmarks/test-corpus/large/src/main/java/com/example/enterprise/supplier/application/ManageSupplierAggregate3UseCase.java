package com.example.enterprise.supplier.application;

import com.example.enterprise.supplier.domain.model.SupplierAggregate3;
import com.example.enterprise.supplier.domain.model.SupplierAggregate3Id;
import com.example.enterprise.supplier.port.driven.SupplierAggregate3Repository;
import com.example.enterprise.supplier.port.driving.SupplierAggregate3Service;
import com.example.enterprise.supplier.port.driving.CreateSupplierAggregate3Command;
import com.example.enterprise.supplier.port.driving.UpdateSupplierAggregate3Command;
import com.example.enterprise.supplier.domain.exception.SupplierAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing SupplierAggregate3.
 */
public class ManageSupplierAggregate3UseCase implements SupplierAggregate3Service {
    private final SupplierAggregate3Repository repository;

    public ManageSupplierAggregate3UseCase(SupplierAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public SupplierAggregate3Id create(CreateSupplierAggregate3Command command) {
        SupplierAggregate3Id id = SupplierAggregate3Id.generate();
        SupplierAggregate3 entity = new SupplierAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public SupplierAggregate3 get(SupplierAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new SupplierAggregate3NotFoundException(id));
    }

    @Override
    public List<SupplierAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateSupplierAggregate3Command command) {
        SupplierAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(SupplierAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new SupplierAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(SupplierAggregate3Id id) {
        SupplierAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(SupplierAggregate3Id id) {
        SupplierAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
