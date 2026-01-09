package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.WarehouseService;
import com.example.ecommerce.port.driving.CreateWarehouseCommand;
import com.example.ecommerce.port.driving.UpdateWarehouseCommand;
import com.example.ecommerce.port.driven.WarehouseRepository;
import com.example.ecommerce.domain.model.Warehouse;
import com.example.ecommerce.domain.model.WarehouseId;
import java.util.List;

/**
 * Use case implementation for Warehouse operations.
 */
public class ManageWarehouseUseCase implements WarehouseService {
    private final WarehouseRepository repository;

    public ManageWarehouseUseCase(WarehouseRepository repository) {
        this.repository = repository;
    }

    @Override
    public WarehouseId create(CreateWarehouseCommand command) {
        Warehouse entity = new Warehouse(
            WarehouseId.generate(),
            command.name()
        );
        Warehouse saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Warehouse getWarehouse(WarehouseId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));
    }

    @Override
    public List<Warehouse> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(WarehouseId id, UpdateWarehouseCommand command) {
        Warehouse entity = getWarehouse(id);
        repository.save(entity);
    }

    @Override
    public void delete(WarehouseId id) {
        repository.deleteById(id);
    }
}
