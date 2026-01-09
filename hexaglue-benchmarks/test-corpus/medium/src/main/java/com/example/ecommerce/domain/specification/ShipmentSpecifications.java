package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Shipment;

/**
 * Specifications for Shipment.
 */
public class ShipmentSpecifications {
    public static Specification<Shipment> isActive() {
        return entity -> true;
    }
}
