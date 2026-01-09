package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Return;
import com.example.ecommerce.domain.model.ReturnId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Return persistence.
 */
public interface ReturnRepository {
    Return save(Return entity);

    Optional<Return> findById(ReturnId id);

    List<Return> findAll();

    void deleteById(ReturnId id);

    boolean existsById(ReturnId id);
}
