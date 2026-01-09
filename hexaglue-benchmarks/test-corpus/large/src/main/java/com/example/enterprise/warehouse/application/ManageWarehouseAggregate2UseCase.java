package com.example.enterprise.warehouse.application;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2Id;
import com.example.enterprise.warehouse.port.driven.WarehouseAggregate2Repository;
import com.example.enterprise.warehouse.port.driving.WarehouseAggregate2Service;
import com.example.enterprise.warehouse.port.driving.CreateWarehouseAggregate2Command;
import com.example.enterprise.warehouse.port.driving.UpdateWarehouseAggregate2Command;
import com.example.enterprise.warehouse.domain.exception.WarehouseAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing WarehouseAggregate2.
 */
public class ManageWarehouseAggregate2UseCase implements WarehouseAggregate2Service {
    private final WarehouseAggregate2Repository repository;

    public ManageWarehouseAggregate2UseCase(WarehouseAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public WarehouseAggregate2Id create(CreateWarehouseAggregate2Command command) {
        WarehouseAggregate2Id id = WarehouseAggregate2Id.generate();
        WarehouseAggregate2 entity = new WarehouseAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public WarehouseAggregate2 get(WarehouseAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new WarehouseAggregate2NotFoundException(id));
    }

    @Override
    public List<WarehouseAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateWarehouseAggregate2Command command) {
        WarehouseAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(WarehouseAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new WarehouseAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(WarehouseAggregate2Id id) {
        WarehouseAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(WarehouseAggregate2Id id) {
        WarehouseAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
