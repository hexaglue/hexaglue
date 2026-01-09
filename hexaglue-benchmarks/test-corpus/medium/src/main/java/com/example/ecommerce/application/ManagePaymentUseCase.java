package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.PaymentService;
import com.example.ecommerce.port.driving.CreatePaymentCommand;
import com.example.ecommerce.port.driving.UpdatePaymentCommand;
import com.example.ecommerce.port.driven.PaymentRepository;
import com.example.ecommerce.domain.model.Payment;
import com.example.ecommerce.domain.model.PaymentId;
import java.util.List;

/**
 * Use case implementation for Payment operations.
 */
public class ManagePaymentUseCase implements PaymentService {
    private final PaymentRepository repository;

    public ManagePaymentUseCase(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentId create(CreatePaymentCommand command) {
        Payment entity = new Payment(
            PaymentId.generate(),
            command.name()
        );
        Payment saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Payment getPayment(PaymentId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    @Override
    public List<Payment> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(PaymentId id, UpdatePaymentCommand command) {
        Payment entity = getPayment(id);
        repository.save(entity);
    }

    @Override
    public void delete(PaymentId id) {
        repository.deleteById(id);
    }
}
