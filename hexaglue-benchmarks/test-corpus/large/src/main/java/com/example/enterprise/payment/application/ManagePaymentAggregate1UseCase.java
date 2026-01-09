package com.example.enterprise.payment.application;

import com.example.enterprise.payment.domain.model.PaymentAggregate1;
import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;
import com.example.enterprise.payment.port.driven.PaymentAggregate1Repository;
import com.example.enterprise.payment.port.driving.PaymentAggregate1Service;
import com.example.enterprise.payment.port.driving.CreatePaymentAggregate1Command;
import com.example.enterprise.payment.port.driving.UpdatePaymentAggregate1Command;
import com.example.enterprise.payment.domain.exception.PaymentAggregate1NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing PaymentAggregate1.
 */
public class ManagePaymentAggregate1UseCase implements PaymentAggregate1Service {
    private final PaymentAggregate1Repository repository;

    public ManagePaymentAggregate1UseCase(PaymentAggregate1Repository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentAggregate1Id create(CreatePaymentAggregate1Command command) {
        PaymentAggregate1Id id = PaymentAggregate1Id.generate();
        PaymentAggregate1 entity = new PaymentAggregate1(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public PaymentAggregate1 get(PaymentAggregate1Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new PaymentAggregate1NotFoundException(id));
    }

    @Override
    public List<PaymentAggregate1> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdatePaymentAggregate1Command command) {
        PaymentAggregate1 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(PaymentAggregate1Id id) {
        if (!repository.existsById(id)) {
            throw new PaymentAggregate1NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(PaymentAggregate1Id id) {
        PaymentAggregate1 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(PaymentAggregate1Id id) {
        PaymentAggregate1 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
