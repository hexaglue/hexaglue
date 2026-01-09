package com.example.enterprise.payment.port.driven;

import com.example.enterprise.payment.domain.model.PaymentAggregate3;
import com.example.enterprise.payment.domain.model.PaymentAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for PaymentAggregate3 persistence.
 */
public interface PaymentAggregate3Repository {
    PaymentAggregate3 save(PaymentAggregate3 entity);
    Optional<PaymentAggregate3> findById(PaymentAggregate3Id id);
    List<PaymentAggregate3> findAll();
    void deleteById(PaymentAggregate3Id id);
    boolean existsById(PaymentAggregate3Id id);
    long count();
}
