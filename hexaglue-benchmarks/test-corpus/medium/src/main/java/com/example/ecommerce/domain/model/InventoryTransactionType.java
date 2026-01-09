package com.example.ecommerce.domain.model;

/**
 * Value Object representing the type of inventory transaction.
 */
public enum InventoryTransactionType {
    STOCK_IN,
    STOCK_OUT,
    RESERVED,
    RELEASED,
    ADJUSTMENT
}
