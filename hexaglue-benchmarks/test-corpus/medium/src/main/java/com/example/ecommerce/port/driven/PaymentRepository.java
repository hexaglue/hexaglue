package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Payment;
import com.example.ecommerce.domain.model.PaymentId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Payment persistence.
 */
public interface PaymentRepository {
    Payment save(Payment entity);

    Optional<Payment> findById(PaymentId id);

    List<Payment> findAll();

    void deleteById(PaymentId id);

    boolean existsById(PaymentId id);
}
