package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.SupplierService;
import com.example.ecommerce.port.driving.CreateSupplierCommand;
import com.example.ecommerce.port.driving.UpdateSupplierCommand;
import com.example.ecommerce.port.driven.SupplierRepository;
import com.example.ecommerce.domain.model.Supplier;
import com.example.ecommerce.domain.model.SupplierId;
import java.util.List;

/**
 * Use case implementation for Supplier operations.
 */
public class ManageSupplierUseCase implements SupplierService {
    private final SupplierRepository repository;

    public ManageSupplierUseCase(SupplierRepository repository) {
        this.repository = repository;
    }

    @Override
    public SupplierId create(CreateSupplierCommand command) {
        Supplier entity = new Supplier(
            SupplierId.generate(),
            command.name()
        );
        Supplier saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Supplier getSupplier(SupplierId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
    }

    @Override
    public List<Supplier> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(SupplierId id, UpdateSupplierCommand command) {
        Supplier entity = getSupplier(id);
        repository.save(entity);
    }

    @Override
    public void delete(SupplierId id) {
        repository.deleteById(id);
    }
}
