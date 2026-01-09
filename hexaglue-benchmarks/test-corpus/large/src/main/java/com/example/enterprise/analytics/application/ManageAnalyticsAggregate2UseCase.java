package com.example.enterprise.analytics.application;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2Id;
import com.example.enterprise.analytics.port.driven.AnalyticsAggregate2Repository;
import com.example.enterprise.analytics.port.driving.AnalyticsAggregate2Service;
import com.example.enterprise.analytics.port.driving.CreateAnalyticsAggregate2Command;
import com.example.enterprise.analytics.port.driving.UpdateAnalyticsAggregate2Command;
import com.example.enterprise.analytics.domain.exception.AnalyticsAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing AnalyticsAggregate2.
 */
public class ManageAnalyticsAggregate2UseCase implements AnalyticsAggregate2Service {
    private final AnalyticsAggregate2Repository repository;

    public ManageAnalyticsAggregate2UseCase(AnalyticsAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public AnalyticsAggregate2Id create(CreateAnalyticsAggregate2Command command) {
        AnalyticsAggregate2Id id = AnalyticsAggregate2Id.generate();
        AnalyticsAggregate2 entity = new AnalyticsAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public AnalyticsAggregate2 get(AnalyticsAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new AnalyticsAggregate2NotFoundException(id));
    }

    @Override
    public List<AnalyticsAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateAnalyticsAggregate2Command command) {
        AnalyticsAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(AnalyticsAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new AnalyticsAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(AnalyticsAggregate2Id id) {
        AnalyticsAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(AnalyticsAggregate2Id id) {
        AnalyticsAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
