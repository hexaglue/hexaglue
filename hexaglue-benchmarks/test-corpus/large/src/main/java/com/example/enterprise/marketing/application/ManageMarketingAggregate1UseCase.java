package com.example.enterprise.marketing.application;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1;
import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;
import com.example.enterprise.marketing.port.driven.MarketingAggregate1Repository;
import com.example.enterprise.marketing.port.driving.MarketingAggregate1Service;
import com.example.enterprise.marketing.port.driving.CreateMarketingAggregate1Command;
import com.example.enterprise.marketing.port.driving.UpdateMarketingAggregate1Command;
import com.example.enterprise.marketing.domain.exception.MarketingAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing MarketingAggregate1.
 */
public class ManageMarketingAggregate1UseCase implements MarketingAggregate1Service {
    private final MarketingAggregate1Repository repository;

    public ManageMarketingAggregate1UseCase(MarketingAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public MarketingAggregate1Id create(CreateMarketingAggregate1Command command) {
        MarketingAggregate1Id id = MarketingAggregate1Id.generate();
        MarketingAggregate1 entity = new MarketingAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public MarketingAggregate1 get(MarketingAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new MarketingAggregate1NotFoundException(id));
    }

    @Override
    public List<MarketingAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateMarketingAggregate1Command command) {
        MarketingAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(MarketingAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new MarketingAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(MarketingAggregate1Id id) {
        MarketingAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(MarketingAggregate1Id id) {
        MarketingAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
