package com.example.enterprise.shipping.application;

import com.example.enterprise.shipping.domain.model.ShippingAggregate2;
import com.example.enterprise.shipping.domain.model.ShippingAggregate2Id;
import com.example.enterprise.shipping.port.driven.ShippingAggregate2Repository;
import com.example.enterprise.shipping.port.driving.ShippingAggregate2Service;
import com.example.enterprise.shipping.port.driving.CreateShippingAggregate2Command;
import com.example.enterprise.shipping.port.driving.UpdateShippingAggregate2Command;
import com.example.enterprise.shipping.domain.exception.ShippingAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing ShippingAggregate2.
 */
public class ManageShippingAggregate2UseCase implements ShippingAggregate2Service {
    private final ShippingAggregate2Repository repository;

    public ManageShippingAggregate2UseCase(ShippingAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public ShippingAggregate2Id create(CreateShippingAggregate2Command command) {
        ShippingAggregate2Id id = ShippingAggregate2Id.generate();
        ShippingAggregate2 entity = new ShippingAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public ShippingAggregate2 get(ShippingAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new ShippingAggregate2NotFoundException(id));
    }

    @Override
    public List<ShippingAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateShippingAggregate2Command command) {
        ShippingAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(ShippingAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new ShippingAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(ShippingAggregate2Id id) {
        ShippingAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(ShippingAggregate2Id id) {
        ShippingAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
