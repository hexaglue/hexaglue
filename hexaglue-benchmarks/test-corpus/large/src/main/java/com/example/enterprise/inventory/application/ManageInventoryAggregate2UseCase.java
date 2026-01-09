package com.example.enterprise.inventory.application;

import com.example.enterprise.inventory.domain.model.InventoryAggregate2;
import com.example.enterprise.inventory.domain.model.InventoryAggregate2Id;
import com.example.enterprise.inventory.port.driven.InventoryAggregate2Repository;
import com.example.enterprise.inventory.port.driving.InventoryAggregate2Service;
import com.example.enterprise.inventory.port.driving.CreateInventoryAggregate2Command;
import com.example.enterprise.inventory.port.driving.UpdateInventoryAggregate2Command;
import com.example.enterprise.inventory.domain.exception.InventoryAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing InventoryAggregate2.
 */
public class ManageInventoryAggregate2UseCase implements InventoryAggregate2Service {
    private final InventoryAggregate2Repository repository;

    public ManageInventoryAggregate2UseCase(InventoryAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryAggregate2Id create(CreateInventoryAggregate2Command command) {
        InventoryAggregate2Id id = InventoryAggregate2Id.generate();
        InventoryAggregate2 entity = new InventoryAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public InventoryAggregate2 get(InventoryAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new InventoryAggregate2NotFoundException(id));
    }

    @Override
    public List<InventoryAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateInventoryAggregate2Command command) {
        InventoryAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(InventoryAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new InventoryAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(InventoryAggregate2Id id) {
        InventoryAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(InventoryAggregate2Id id) {
        InventoryAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
