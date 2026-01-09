package com.example.enterprise.shipping.application;

import com.example.enterprise.shipping.domain.model.ShippingAggregate3;
import com.example.enterprise.shipping.domain.model.ShippingAggregate3Id;
import com.example.enterprise.shipping.port.driven.ShippingAggregate3Repository;
import com.example.enterprise.shipping.port.driving.ShippingAggregate3Service;
import com.example.enterprise.shipping.port.driving.CreateShippingAggregate3Command;
import com.example.enterprise.shipping.port.driving.UpdateShippingAggregate3Command;
import com.example.enterprise.shipping.domain.exception.ShippingAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing ShippingAggregate3.
 */
public class ManageShippingAggregate3UseCase implements ShippingAggregate3Service {
    private final ShippingAggregate3Repository repository;

    public ManageShippingAggregate3UseCase(ShippingAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public ShippingAggregate3Id create(CreateShippingAggregate3Command command) {
        ShippingAggregate3Id id = ShippingAggregate3Id.generate();
        ShippingAggregate3 entity = new ShippingAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public ShippingAggregate3 get(ShippingAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new ShippingAggregate3NotFoundException(id));
    }

    @Override
    public List<ShippingAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateShippingAggregate3Command command) {
        ShippingAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(ShippingAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new ShippingAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(ShippingAggregate3Id id) {
        ShippingAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(ShippingAggregate3Id id) {
        ShippingAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
