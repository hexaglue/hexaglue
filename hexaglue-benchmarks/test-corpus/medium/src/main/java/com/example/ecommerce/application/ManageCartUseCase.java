package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.CartService;
import com.example.ecommerce.port.driving.CreateCartCommand;
import com.example.ecommerce.port.driving.UpdateCartCommand;
import com.example.ecommerce.port.driven.CartRepository;
import com.example.ecommerce.domain.model.Cart;
import com.example.ecommerce.domain.model.CartId;
import java.util.List;

/**
 * Use case implementation for Cart operations.
 */
public class ManageCartUseCase implements CartService {
    private final CartRepository repository;

    public ManageCartUseCase(CartRepository repository) {
        this.repository = repository;
    }

    @Override
    public CartId create(CreateCartCommand command) {
        Cart entity = new Cart(
            CartId.generate(),
            command.name()
        );
        Cart saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Cart getCart(CartId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + id));
    }

    @Override
    public List<Cart> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(CartId id, UpdateCartCommand command) {
        Cart entity = getCart(id);
        repository.save(entity);
    }

    @Override
    public void delete(CartId id) {
        repository.deleteById(id);
    }
}
