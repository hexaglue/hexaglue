package com.example.enterprise.analytics.application;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;
import com.example.enterprise.analytics.port.driven.AnalyticsAggregate1Repository;
import com.example.enterprise.analytics.port.driving.AnalyticsAggregate1Service;
import com.example.enterprise.analytics.port.driving.CreateAnalyticsAggregate1Command;
import com.example.enterprise.analytics.port.driving.UpdateAnalyticsAggregate1Command;
import com.example.enterprise.analytics.domain.exception.AnalyticsAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing AnalyticsAggregate1.
 */
public class ManageAnalyticsAggregate1UseCase implements AnalyticsAggregate1Service {
    private final AnalyticsAggregate1Repository repository;

    public ManageAnalyticsAggregate1UseCase(AnalyticsAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public AnalyticsAggregate1Id create(CreateAnalyticsAggregate1Command command) {
        AnalyticsAggregate1Id id = AnalyticsAggregate1Id.generate();
        AnalyticsAggregate1 entity = new AnalyticsAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public AnalyticsAggregate1 get(AnalyticsAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new AnalyticsAggregate1NotFoundException(id));
    }

    @Override
    public List<AnalyticsAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateAnalyticsAggregate1Command command) {
        AnalyticsAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(AnalyticsAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new AnalyticsAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(AnalyticsAggregate1Id id) {
        AnalyticsAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(AnalyticsAggregate1Id id) {
        AnalyticsAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
