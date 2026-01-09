package com.example.enterprise.marketing.application;

import com.example.enterprise.marketing.domain.model.MarketingAggregate3;
import com.example.enterprise.marketing.domain.model.MarketingAggregate3Id;
import com.example.enterprise.marketing.port.driven.MarketingAggregate3Repository;
import com.example.enterprise.marketing.port.driving.MarketingAggregate3Service;
import com.example.enterprise.marketing.port.driving.CreateMarketingAggregate3Command;
import com.example.enterprise.marketing.port.driving.UpdateMarketingAggregate3Command;
import com.example.enterprise.marketing.domain.exception.MarketingAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing MarketingAggregate3.
 */
public class ManageMarketingAggregate3UseCase implements MarketingAggregate3Service {
    private final MarketingAggregate3Repository repository;

    public ManageMarketingAggregate3UseCase(MarketingAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public MarketingAggregate3Id create(CreateMarketingAggregate3Command command) {
        MarketingAggregate3Id id = MarketingAggregate3Id.generate();
        MarketingAggregate3 entity = new MarketingAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public MarketingAggregate3 get(MarketingAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new MarketingAggregate3NotFoundException(id));
    }

    @Override
    public List<MarketingAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateMarketingAggregate3Command command) {
        MarketingAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(MarketingAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new MarketingAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(MarketingAggregate3Id id) {
        MarketingAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(MarketingAggregate3Id id) {
        MarketingAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
