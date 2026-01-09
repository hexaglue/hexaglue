package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.WishlistId;
import com.example.ecommerce.domain.model.Wishlist;
import java.util.List;

/**
 * Driving port (primary) for wishlist operations.
 */
public interface WishlistService {
    WishlistId create(CreateWishlistCommand command);

    Wishlist getWishlist(WishlistId id);

    List<Wishlist> getAll();

    void update(WishlistId id, UpdateWishlistCommand command);

    void delete(WishlistId id);
}
