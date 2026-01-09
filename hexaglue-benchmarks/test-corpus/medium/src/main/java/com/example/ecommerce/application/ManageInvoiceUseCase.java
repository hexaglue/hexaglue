package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.InvoiceService;
import com.example.ecommerce.port.driving.CreateInvoiceCommand;
import com.example.ecommerce.port.driving.UpdateInvoiceCommand;
import com.example.ecommerce.port.driven.InvoiceRepository;
import com.example.ecommerce.domain.model.Invoice;
import com.example.ecommerce.domain.model.InvoiceId;
import java.util.List;

/**
 * Use case implementation for Invoice operations.
 */
public class ManageInvoiceUseCase implements InvoiceService {
    private final InvoiceRepository repository;

    public ManageInvoiceUseCase(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public InvoiceId create(CreateInvoiceCommand command) {
        Invoice entity = new Invoice(
            InvoiceId.generate(),
            command.name()
        );
        Invoice saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Invoice getInvoice(InvoiceId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Override
    public List<Invoice> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(InvoiceId id, UpdateInvoiceCommand command) {
        Invoice entity = getInvoice(id);
        repository.save(entity);
    }

    @Override
    public void delete(InvoiceId id) {
        repository.deleteById(id);
    }
}
