package com.example.enterprise.payment.application;

import com.example.enterprise.payment.domain.model.PaymentAggregate2;
import com.example.enterprise.payment.domain.model.PaymentAggregate2Id;
import com.example.enterprise.payment.port.driven.PaymentAggregate2Repository;
import com.example.enterprise.payment.port.driving.PaymentAggregate2Service;
import com.example.enterprise.payment.port.driving.CreatePaymentAggregate2Command;
import com.example.enterprise.payment.port.driving.UpdatePaymentAggregate2Command;
import com.example.enterprise.payment.domain.exception.PaymentAggregate2NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing PaymentAggregate2.
 */
public class ManagePaymentAggregate2UseCase implements PaymentAggregate2Service {
    private final PaymentAggregate2Repository repository;

    public ManagePaymentAggregate2UseCase(PaymentAggregate2Repository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentAggregate2Id create(CreatePaymentAggregate2Command command) {
        PaymentAggregate2Id id = PaymentAggregate2Id.generate();
        PaymentAggregate2 entity = new PaymentAggregate2(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public PaymentAggregate2 get(PaymentAggregate2Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new PaymentAggregate2NotFoundException(id));
    }

    @Override
    public List<PaymentAggregate2> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdatePaymentAggregate2Command command) {
        PaymentAggregate2 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(PaymentAggregate2Id id) {
        if (!repository.existsById(id)) {
            throw new PaymentAggregate2NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(PaymentAggregate2Id id) {
        PaymentAggregate2 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(PaymentAggregate2Id id) {
        PaymentAggregate2 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
