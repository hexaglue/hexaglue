package com.example.enterprise.inventory.application;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1;
import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;
import com.example.enterprise.inventory.port.driven.InventoryAggregate1Repository;
import com.example.enterprise.inventory.port.driving.InventoryAggregate1Service;
import com.example.enterprise.inventory.port.driving.CreateInventoryAggregate1Command;
import com.example.enterprise.inventory.port.driving.UpdateInventoryAggregate1Command;
import com.example.enterprise.inventory.domain.exception.InventoryAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing InventoryAggregate1.
 */
public class ManageInventoryAggregate1UseCase implements InventoryAggregate1Service {
    private final InventoryAggregate1Repository repository;

    public ManageInventoryAggregate1UseCase(InventoryAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryAggregate1Id create(CreateInventoryAggregate1Command command) {
        InventoryAggregate1Id id = InventoryAggregate1Id.generate();
        InventoryAggregate1 entity = new InventoryAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public InventoryAggregate1 get(InventoryAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new InventoryAggregate1NotFoundException(id));
    }

    @Override
    public List<InventoryAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateInventoryAggregate1Command command) {
        InventoryAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(InventoryAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new InventoryAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(InventoryAggregate1Id id) {
        InventoryAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(InventoryAggregate1Id id) {
        InventoryAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
