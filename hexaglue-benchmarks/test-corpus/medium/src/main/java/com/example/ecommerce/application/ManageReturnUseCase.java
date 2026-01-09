package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.ReturnService;
import com.example.ecommerce.port.driving.CreateReturnCommand;
import com.example.ecommerce.port.driving.UpdateReturnCommand;
import com.example.ecommerce.port.driven.ReturnRepository;
import com.example.ecommerce.domain.model.Return;
import com.example.ecommerce.domain.model.ReturnId;
import java.util.List;

/**
 * Use case implementation for Return operations.
 */
public class ManageReturnUseCase implements ReturnService {
    private final ReturnRepository repository;

    public ManageReturnUseCase(ReturnRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReturnId create(CreateReturnCommand command) {
        Return entity = new Return(
            ReturnId.generate(),
            command.name()
        );
        Return saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Return getReturn(ReturnId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + id));
    }

    @Override
    public List<Return> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(ReturnId id, UpdateReturnCommand command) {
        Return entity = getReturn(id);
        repository.save(entity);
    }

    @Override
    public void delete(ReturnId id) {
        repository.deleteById(id);
    }
}
