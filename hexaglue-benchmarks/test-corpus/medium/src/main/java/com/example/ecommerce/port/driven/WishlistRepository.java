package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Wishlist;
import com.example.ecommerce.domain.model.WishlistId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Wishlist persistence.
 */
public interface WishlistRepository {
    Wishlist save(Wishlist entity);

    Optional<Wishlist> findById(WishlistId id);

    List<Wishlist> findAll();

    void deleteById(WishlistId id);

    boolean existsById(WishlistId id);
}
