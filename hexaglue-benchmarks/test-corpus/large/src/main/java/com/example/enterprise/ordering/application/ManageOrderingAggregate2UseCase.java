package com.example.enterprise.ordering.application;

import com.example.enterprise.ordering.domain.model.OrderingAggregate2;
import com.example.enterprise.ordering.domain.model.OrderingAggregate2Id;
import com.example.enterprise.ordering.port.driven.OrderingAggregate2Repository;
import com.example.enterprise.ordering.port.driving.OrderingAggregate2Service;
import com.example.enterprise.ordering.port.driving.CreateOrderingAggregate2Command;
import com.example.enterprise.ordering.port.driving.UpdateOrderingAggregate2Command;
import com.example.enterprise.ordering.domain.exception.OrderingAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing OrderingAggregate2.
 */
public class ManageOrderingAggregate2UseCase implements OrderingAggregate2Service {
    private final OrderingAggregate2Repository repository;

    public ManageOrderingAggregate2UseCase(OrderingAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public OrderingAggregate2Id create(CreateOrderingAggregate2Command command) {
        OrderingAggregate2Id id = OrderingAggregate2Id.generate();
        OrderingAggregate2 entity = new OrderingAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public OrderingAggregate2 get(OrderingAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new OrderingAggregate2NotFoundException(id));
    }

    @Override
    public List<OrderingAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateOrderingAggregate2Command command) {
        OrderingAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(OrderingAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new OrderingAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(OrderingAggregate2Id id) {
        OrderingAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(OrderingAggregate2Id id) {
        OrderingAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
