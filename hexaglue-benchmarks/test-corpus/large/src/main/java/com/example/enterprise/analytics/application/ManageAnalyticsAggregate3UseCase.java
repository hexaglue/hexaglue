package com.example.enterprise.analytics.application;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3Id;
import com.example.enterprise.analytics.port.driven.AnalyticsAggregate3Repository;
import com.example.enterprise.analytics.port.driving.AnalyticsAggregate3Service;
import com.example.enterprise.analytics.port.driving.CreateAnalyticsAggregate3Command;
import com.example.enterprise.analytics.port.driving.UpdateAnalyticsAggregate3Command;
import com.example.enterprise.analytics.domain.exception.AnalyticsAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing AnalyticsAggregate3.
 */
public class ManageAnalyticsAggregate3UseCase implements AnalyticsAggregate3Service {
    private final AnalyticsAggregate3Repository repository;

    public ManageAnalyticsAggregate3UseCase(AnalyticsAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public AnalyticsAggregate3Id create(CreateAnalyticsAggregate3Command command) {
        AnalyticsAggregate3Id id = AnalyticsAggregate3Id.generate();
        AnalyticsAggregate3 entity = new AnalyticsAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public AnalyticsAggregate3 get(AnalyticsAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new AnalyticsAggregate3NotFoundException(id));
    }

    @Override
    public List<AnalyticsAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateAnalyticsAggregate3Command command) {
        AnalyticsAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(AnalyticsAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new AnalyticsAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(AnalyticsAggregate3Id id) {
        AnalyticsAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(AnalyticsAggregate3Id id) {
        AnalyticsAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
