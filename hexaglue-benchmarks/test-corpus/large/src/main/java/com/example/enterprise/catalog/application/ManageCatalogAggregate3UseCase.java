package com.example.enterprise.catalog.application;

import com.example.enterprise.catalog.domain.model.CatalogAggregate3;
import com.example.enterprise.catalog.domain.model.CatalogAggregate3Id;
import com.example.enterprise.catalog.port.driven.CatalogAggregate3Repository;
import com.example.enterprise.catalog.port.driving.CatalogAggregate3Service;
import com.example.enterprise.catalog.port.driving.CreateCatalogAggregate3Command;
import com.example.enterprise.catalog.port.driving.UpdateCatalogAggregate3Command;
import com.example.enterprise.catalog.domain.exception.CatalogAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing CatalogAggregate3.
 */
public class ManageCatalogAggregate3UseCase implements CatalogAggregate3Service {
    private final CatalogAggregate3Repository repository;

    public ManageCatalogAggregate3UseCase(CatalogAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public CatalogAggregate3Id create(CreateCatalogAggregate3Command command) {
        CatalogAggregate3Id id = CatalogAggregate3Id.generate();
        CatalogAggregate3 entity = new CatalogAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public CatalogAggregate3 get(CatalogAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new CatalogAggregate3NotFoundException(id));
    }

    @Override
    public List<CatalogAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateCatalogAggregate3Command command) {
        CatalogAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(CatalogAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new CatalogAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(CatalogAggregate3Id id) {
        CatalogAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(CatalogAggregate3Id id) {
        CatalogAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
