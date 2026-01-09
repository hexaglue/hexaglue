package com.example.enterprise.inventory.application;

import com.example.enterprise.inventory.domain.model.InventoryAggregate3;
import com.example.enterprise.inventory.domain.model.InventoryAggregate3Id;
import com.example.enterprise.inventory.port.driven.InventoryAggregate3Repository;
import com.example.enterprise.inventory.port.driving.InventoryAggregate3Service;
import com.example.enterprise.inventory.port.driving.CreateInventoryAggregate3Command;
import com.example.enterprise.inventory.port.driving.UpdateInventoryAggregate3Command;
import com.example.enterprise.inventory.domain.exception.InventoryAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing InventoryAggregate3.
 */
public class ManageInventoryAggregate3UseCase implements InventoryAggregate3Service {
    private final InventoryAggregate3Repository repository;

    public ManageInventoryAggregate3UseCase(InventoryAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryAggregate3Id create(CreateInventoryAggregate3Command command) {
        InventoryAggregate3Id id = InventoryAggregate3Id.generate();
        InventoryAggregate3 entity = new InventoryAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public InventoryAggregate3 get(InventoryAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new InventoryAggregate3NotFoundException(id));
    }

    @Override
    public List<InventoryAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateInventoryAggregate3Command command) {
        InventoryAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(InventoryAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new InventoryAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(InventoryAggregate3Id id) {
        InventoryAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(InventoryAggregate3Id id) {
        InventoryAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
