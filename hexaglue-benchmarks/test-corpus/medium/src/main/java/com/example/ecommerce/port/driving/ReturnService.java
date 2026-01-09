package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.ReturnId;
import com.example.ecommerce.domain.model.Return;
import java.util.List;

/**
 * Driving port (primary) for return operations.
 */
public interface ReturnService {
    ReturnId create(CreateReturnCommand command);

    Return getReturn(ReturnId id);

    List<Return> getAll();

    void update(ReturnId id, UpdateReturnCommand command);

    void delete(ReturnId id);
}
