package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Invoice;
import com.example.ecommerce.domain.model.InvoiceId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Invoice persistence.
 */
public interface InvoiceRepository {
    Invoice save(Invoice entity);

    Optional<Invoice> findById(InvoiceId id);

    List<Invoice> findAll();

    void deleteById(InvoiceId id);

    boolean existsById(InvoiceId id);
}
