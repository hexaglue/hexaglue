package com.example.ecommerce.domain.model;

/**
 * Entity representing An item in a wishlist.
 */
public class WishlistItem {
    private final String id;
    private String name;

    public WishlistItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
