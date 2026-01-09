package com.example.enterprise.marketing.application;

import com.example.enterprise.marketing.domain.model.MarketingAggregate2;
import com.example.enterprise.marketing.domain.model.MarketingAggregate2Id;
import com.example.enterprise.marketing.port.driven.MarketingAggregate2Repository;
import com.example.enterprise.marketing.port.driving.MarketingAggregate2Service;
import com.example.enterprise.marketing.port.driving.CreateMarketingAggregate2Command;
import com.example.enterprise.marketing.port.driving.UpdateMarketingAggregate2Command;
import com.example.enterprise.marketing.domain.exception.MarketingAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing MarketingAggregate2.
 */
public class ManageMarketingAggregate2UseCase implements MarketingAggregate2Service {
    private final MarketingAggregate2Repository repository;

    public ManageMarketingAggregate2UseCase(MarketingAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public MarketingAggregate2Id create(CreateMarketingAggregate2Command command) {
        MarketingAggregate2Id id = MarketingAggregate2Id.generate();
        MarketingAggregate2 entity = new MarketingAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public MarketingAggregate2 get(MarketingAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new MarketingAggregate2NotFoundException(id));
    }

    @Override
    public List<MarketingAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateMarketingAggregate2Command command) {
        MarketingAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(MarketingAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new MarketingAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(MarketingAggregate2Id id) {
        MarketingAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(MarketingAggregate2Id id) {
        MarketingAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
