package com.example.enterprise.payment.port.driven;

import com.example.enterprise.payment.domain.model.PaymentAggregate2;
import com.example.enterprise.payment.domain.model.PaymentAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for PaymentAggregate2 persistence.
 */
public interface PaymentAggregate2Repository {
    PaymentAggregate2 save(PaymentAggregate2 entity);
    Optional<PaymentAggregate2> findById(PaymentAggregate2Id id);
    List<PaymentAggregate2> findAll();
    void deleteById(PaymentAggregate2Id id);
    boolean existsById(PaymentAggregate2Id id);
    long count();
}
