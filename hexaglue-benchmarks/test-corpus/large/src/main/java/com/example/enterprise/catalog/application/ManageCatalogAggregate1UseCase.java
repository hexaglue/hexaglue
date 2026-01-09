package com.example.enterprise.catalog.application;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1;
import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;
import com.example.enterprise.catalog.port.driven.CatalogAggregate1Repository;
import com.example.enterprise.catalog.port.driving.CatalogAggregate1Service;
import com.example.enterprise.catalog.port.driving.CreateCatalogAggregate1Command;
import com.example.enterprise.catalog.port.driving.UpdateCatalogAggregate1Command;
import com.example.enterprise.catalog.domain.exception.CatalogAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CatalogAggregate1.
 */
public class ManageCatalogAggregate1UseCase implements CatalogAggregate1Service {
    private final CatalogAggregate1Repository repository;

    public ManageCatalogAggregate1UseCase(CatalogAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public CatalogAggregate1Id create(CreateCatalogAggregate1Command command) {
        CatalogAggregate1Id id = CatalogAggregate1Id.generate();
        CatalogAggregate1 entity = new CatalogAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CatalogAggregate1 get(CatalogAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CatalogAggregate1NotFoundException(id));
    }

    @Override
    public List<CatalogAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCatalogAggregate1Command command) {
        CatalogAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CatalogAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new CatalogAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CatalogAggregate1Id id) {
        CatalogAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CatalogAggregate1Id id) {
        CatalogAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
