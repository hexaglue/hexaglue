package com.example.enterprise.shipping.application;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1;
import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;
import com.example.enterprise.shipping.port.driven.ShippingAggregate1Repository;
import com.example.enterprise.shipping.port.driving.ShippingAggregate1Service;
import com.example.enterprise.shipping.port.driving.CreateShippingAggregate1Command;
import com.example.enterprise.shipping.port.driving.UpdateShippingAggregate1Command;
import com.example.enterprise.shipping.domain.exception.ShippingAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing ShippingAggregate1.
 */
public class ManageShippingAggregate1UseCase implements ShippingAggregate1Service {
    private final ShippingAggregate1Repository repository;

    public ManageShippingAggregate1UseCase(ShippingAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public ShippingAggregate1Id create(CreateShippingAggregate1Command command) {
        ShippingAggregate1Id id = ShippingAggregate1Id.generate();
        ShippingAggregate1 entity = new ShippingAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public ShippingAggregate1 get(ShippingAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new ShippingAggregate1NotFoundException(id));
    }

    @Override
    public List<ShippingAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateShippingAggregate1Command command) {
        ShippingAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(ShippingAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new ShippingAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(ShippingAggregate1Id id) {
        ShippingAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(ShippingAggregate1Id id) {
        ShippingAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
