package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.ShipmentService;
import com.example.ecommerce.port.driving.CreateShipmentCommand;
import com.example.ecommerce.port.driving.UpdateShipmentCommand;
import com.example.ecommerce.port.driven.ShipmentRepository;
import com.example.ecommerce.domain.model.Shipment;
import com.example.ecommerce.domain.model.ShipmentId;
import java.util.List;

/**
 * Use case implementation for Shipment operations.
 */
public class ManageShipmentUseCase implements ShipmentService {
    private final ShipmentRepository repository;

    public ManageShipmentUseCase(ShipmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public ShipmentId create(CreateShipmentCommand command) {
        Shipment entity = new Shipment(
            ShipmentId.generate(),
            command.name()
        );
        Shipment saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Shipment getShipment(ShipmentId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + id));
    }

    @Override
    public List<Shipment> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(ShipmentId id, UpdateShipmentCommand command) {
        Shipment entity = getShipment(id);
        repository.save(entity);
    }

    @Override
    public void delete(ShipmentId id) {
        repository.deleteById(id);
    }
}
