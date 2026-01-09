package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.InvoiceId;
import com.example.ecommerce.domain.model.Invoice;
import java.util.List;

/**
 * Driving port (primary) for invoice operations.
 */
public interface InvoiceService {
    InvoiceId create(CreateInvoiceCommand command);

    Invoice getInvoice(InvoiceId id);

    List<Invoice> getAll();

    void update(InvoiceId id, UpdateInvoiceCommand command);

    void delete(InvoiceId id);
}
