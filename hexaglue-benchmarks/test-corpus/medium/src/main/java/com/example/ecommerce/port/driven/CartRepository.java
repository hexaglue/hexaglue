package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Cart;
import com.example.ecommerce.domain.model.CartId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Cart persistence.
 */
public interface CartRepository {
    Cart save(Cart entity);

    Optional<Cart> findById(CartId id);

    List<Cart> findAll();

    void deleteById(CartId id);

    boolean existsById(CartId id);
}
