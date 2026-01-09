package com.example.enterprise.catalog.application;

import com.example.enterprise.catalog.domain.model.CatalogAggregate2;
import com.example.enterprise.catalog.domain.model.CatalogAggregate2Id;
import com.example.enterprise.catalog.port.driven.CatalogAggregate2Repository;
import com.example.enterprise.catalog.port.driving.CatalogAggregate2Service;
import com.example.enterprise.catalog.port.driving.CreateCatalogAggregate2Command;
import com.example.enterprise.catalog.port.driving.UpdateCatalogAggregate2Command;
import com.example.enterprise.catalog.domain.exception.CatalogAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CatalogAggregate2.
 */
public class ManageCatalogAggregate2UseCase implements CatalogAggregate2Service {
    private final CatalogAggregate2Repository repository;

    public ManageCatalogAggregate2UseCase(CatalogAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public CatalogAggregate2Id create(CreateCatalogAggregate2Command command) {
        CatalogAggregate2Id id = CatalogAggregate2Id.generate();
        CatalogAggregate2 entity = new CatalogAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CatalogAggregate2 get(CatalogAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CatalogAggregate2NotFoundException(id));
    }

    @Override
    public List<CatalogAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCatalogAggregate2Command command) {
        CatalogAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CatalogAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new CatalogAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CatalogAggregate2Id id) {
        CatalogAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CatalogAggregate2Id id) {
        CatalogAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
