package com.example.enterprise.ordering.application;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1;
import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;
import com.example.enterprise.ordering.port.driven.OrderingAggregate1Repository;
import com.example.enterprise.ordering.port.driving.OrderingAggregate1Service;
import com.example.enterprise.ordering.port.driving.CreateOrderingAggregate1Command;
import com.example.enterprise.ordering.port.driving.UpdateOrderingAggregate1Command;
import com.example.enterprise.ordering.domain.exception.OrderingAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing OrderingAggregate1.
 */
public class ManageOrderingAggregate1UseCase implements OrderingAggregate1Service {
    private final OrderingAggregate1Repository repository;

    public ManageOrderingAggregate1UseCase(OrderingAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public OrderingAggregate1Id create(CreateOrderingAggregate1Command command) {
        OrderingAggregate1Id id = OrderingAggregate1Id.generate();
        OrderingAggregate1 entity = new OrderingAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public OrderingAggregate1 get(OrderingAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new OrderingAggregate1NotFoundException(id));
    }

    @Override
    public List<OrderingAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdateOrderingAggregate1Command command) {
        OrderingAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(OrderingAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new OrderingAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(OrderingAggregate1Id id) {
        OrderingAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(OrderingAggregate1Id id) {
        OrderingAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
