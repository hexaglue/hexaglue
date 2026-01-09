package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.CartId;
import com.example.ecommerce.domain.model.Cart;
import java.util.List;

/**
 * Driving port (primary) for cart operations.
 */
public interface CartService {
    CartId create(CreateCartCommand command);

    Cart getCart(CartId id);

    List<Cart> getAll();

    void update(CartId id, UpdateCartCommand command);

    void delete(CartId id);
}
