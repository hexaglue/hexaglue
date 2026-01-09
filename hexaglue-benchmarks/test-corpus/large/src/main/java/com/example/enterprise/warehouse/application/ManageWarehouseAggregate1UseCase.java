package com.example.enterprise.warehouse.application;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;
import com.example.enterprise.warehouse.port.driven.WarehouseAggregate1Repository;
import com.example.enterprise.warehouse.port.driving.WarehouseAggregate1Service;
import com.example.enterprise.warehouse.port.driving.CreateWarehouseAggregate1Command;
import com.example.enterprise.warehouse.port.driving.UpdateWarehouseAggregate1Command;
import com.example.enterprise.warehouse.domain.exception.WarehouseAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing WarehouseAggregate1.
 */
public class ManageWarehouseAggregate1UseCase implements WarehouseAggregate1Service {
    private final WarehouseAggregate1Repository repository;

    public ManageWarehouseAggregate1UseCase(WarehouseAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public WarehouseAggregate1Id create(CreateWarehouseAggregate1Command command) {
        WarehouseAggregate1Id id = WarehouseAggregate1Id.generate();
        WarehouseAggregate1 entity = new WarehouseAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public WarehouseAggregate1 get(WarehouseAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new WarehouseAggregate1NotFoundException(id));
    }

    @Override
    public List<WarehouseAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateWarehouseAggregate1Command command) {
        WarehouseAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(WarehouseAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new WarehouseAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(WarehouseAggregate1Id id) {
        WarehouseAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(WarehouseAggregate1Id id) {
        WarehouseAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
