package com.example.enterprise.warehouse.application;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3Id;
import com.example.enterprise.warehouse.port.driven.WarehouseAggregate3Repository;
import com.example.enterprise.warehouse.port.driving.WarehouseAggregate3Service;
import com.example.enterprise.warehouse.port.driving.CreateWarehouseAggregate3Command;
import com.example.enterprise.warehouse.port.driving.UpdateWarehouseAggregate3Command;
import com.example.enterprise.warehouse.domain.exception.WarehouseAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing WarehouseAggregate3.
 */
public class ManageWarehouseAggregate3UseCase implements WarehouseAggregate3Service {
    private final WarehouseAggregate3Repository repository;

    public ManageWarehouseAggregate3UseCase(WarehouseAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public WarehouseAggregate3Id create(CreateWarehouseAggregate3Command command) {
        WarehouseAggregate3Id id = WarehouseAggregate3Id.generate();
        WarehouseAggregate3 entity = new WarehouseAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public WarehouseAggregate3 get(WarehouseAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new WarehouseAggregate3NotFoundException(id));
    }

    @Override
    public List<WarehouseAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateWarehouseAggregate3Command command) {
        WarehouseAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(WarehouseAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new WarehouseAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(WarehouseAggregate3Id id) {
        WarehouseAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(WarehouseAggregate3Id id) {
        WarehouseAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
