package com.example.enterprise.payment.port.driven;

import com.example.enterprise.payment.domain.model.PaymentAggregate1;
import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for PaymentAggregate1 persistence.
 */
public interface PaymentAggregate1Repository {
    PaymentAggregate1 save(PaymentAggregate1 entity);
    Optional<PaymentAggregate1> findById(PaymentAggregate1Id id);
    List<PaymentAggregate1> findAll();
    void deleteById(PaymentAggregate1Id id);
    boolean existsById(PaymentAggregate1Id id);
    long count();
}
