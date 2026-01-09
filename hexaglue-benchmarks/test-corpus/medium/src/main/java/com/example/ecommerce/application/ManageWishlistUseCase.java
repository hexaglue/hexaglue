package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.WishlistService;
import com.example.ecommerce.port.driving.CreateWishlistCommand;
import com.example.ecommerce.port.driving.UpdateWishlistCommand;
import com.example.ecommerce.port.driven.WishlistRepository;
import com.example.ecommerce.domain.model.Wishlist;
import com.example.ecommerce.domain.model.WishlistId;
import java.util.List;

/**
 * Use case implementation for Wishlist operations.
 */
public class ManageWishlistUseCase implements WishlistService {
    private final WishlistRepository repository;

    public ManageWishlistUseCase(WishlistRepository repository) {
        this.repository = repository;
    }

    @Override
    public WishlistId create(CreateWishlistCommand command) {
        Wishlist entity = new Wishlist(
            WishlistId.generate(),
            command.name()
        );
        Wishlist saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Wishlist getWishlist(WishlistId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Wishlist not found: " + id));
    }

    @Override
    public List<Wishlist> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(WishlistId id, UpdateWishlistCommand command) {
        Wishlist entity = getWishlist(id);
        repository.save(entity);
    }

    @Override
    public void delete(WishlistId id) {
        repository.deleteById(id);
    }
}
