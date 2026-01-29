package com.example.ecommerce.application.port.driving;

import com.example.ecommerce.domain.product.ProductId;

/**
 * Driving port for inventory availability and reservation operations.
 *
 * <p>This port exposes inventory-related use cases to external actors, including
 * stock availability checks, reservation for pending orders, and release of
 * reserved stock when orders are cancelled.
 *
 * <p>AUDIT VIOLATION: hex:port-interface.
 * This port is incorrectly defined as a concrete class instead of an interface.
 * Ports should always be interfaces to allow multiple implementations and
 * respect the dependency inversion principle.
 */
public class InventoryPort {

    /**
     * Checks if a product has available stock.
     */
    public boolean checkAvailability(ProductId productId, int quantity) {
        // This should be an interface method, not a concrete implementation
        throw new UnsupportedOperationException("This is a violation - ports should be interfaces");
    }

    /**
     * Reserves inventory for an order.
     */
    public void reserveInventory(ProductId productId, int quantity) {
        throw new UnsupportedOperationException("This is a violation - ports should be interfaces");
    }

    /**
     * Releases reserved inventory.
     */
    public void releaseInventory(ProductId productId, int quantity) {
        throw new UnsupportedOperationException("This is a violation - ports should be interfaces");
    }
}
