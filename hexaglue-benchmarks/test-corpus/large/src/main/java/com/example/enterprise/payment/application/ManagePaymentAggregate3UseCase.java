package com.example.enterprise.payment.application;

import com.example.enterprise.payment.domain.model.PaymentAggregate3;
import com.example.enterprise.payment.domain.model.PaymentAggregate3Id;
import com.example.enterprise.payment.port.driven.PaymentAggregate3Repository;
import com.example.enterprise.payment.port.driving.PaymentAggregate3Service;
import com.example.enterprise.payment.port.driving.CreatePaymentAggregate3Command;
import com.example.enterprise.payment.port.driving.UpdatePaymentAggregate3Command;
import com.example.enterprise.payment.domain.exception.PaymentAggregate3NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing PaymentAggregate3.
 */
public class ManagePaymentAggregate3UseCase implements PaymentAggregate3Service {
    private final PaymentAggregate3Repository repository;

    public ManagePaymentAggregate3UseCase(PaymentAggregate3Repository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentAggregate3Id create(CreatePaymentAggregate3Command command) {
        PaymentAggregate3Id id = PaymentAggregate3Id.generate();
        PaymentAggregate3 entity = new PaymentAggregate3(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public PaymentAggregate3 get(PaymentAggregate3Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new PaymentAggregate3NotFoundException(id));
    }

    @Override
    public List<PaymentAggregate3> list() {
        return repository.findAll();
    }

    @Override
    public void update(UpdatePaymentAggregate3Command command) {
        PaymentAggregate3 entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(PaymentAggregate3Id id) {
        if (!repository.existsById(id)) {
            throw new PaymentAggregate3NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(PaymentAggregate3Id id) {
        PaymentAggregate3 entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(PaymentAggregate3Id id) {
        PaymentAggregate3 entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
