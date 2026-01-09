package com.example.enterprise.ordering.application;

import com.example.enterprise.ordering.domain.model.OrderingAggregate3;
import com.example.enterprise.ordering.domain.model.OrderingAggregate3Id;
import com.example.enterprise.ordering.port.driven.OrderingAggregate3Repository;
import com.example.enterprise.ordering.port.driving.OrderingAggregate3Service;
import com.example.enterprise.ordering.port.driving.CreateOrderingAggregate3Command;
import com.example.enterprise.ordering.port.driving.UpdateOrderingAggregate3Command;
import com.example.enterprise.ordering.domain.exception.OrderingAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing OrderingAggregate3.
 */
public class ManageOrderingAggregate3UseCase implements OrderingAggregate3Service {
    private final OrderingAggregate3Repository repository;

    public ManageOrderingAggregate3UseCase(OrderingAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public OrderingAggregate3Id create(CreateOrderingAggregate3Command command) {
        OrderingAggregate3Id id = OrderingAggregate3Id.generate();
        OrderingAggregate3 entity = new OrderingAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public OrderingAggregate3 get(OrderingAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new OrderingAggregate3NotFoundException(id));
    }

    @Override
    public List<OrderingAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateOrderingAggregate3Command command) {
        OrderingAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(OrderingAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new OrderingAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(OrderingAggregate3Id id) {
        OrderingAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(OrderingAggregate3Id id) {
        OrderingAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
