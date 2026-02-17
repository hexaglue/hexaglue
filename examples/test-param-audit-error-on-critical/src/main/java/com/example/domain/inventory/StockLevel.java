package com.example.domain.inventory;

/** Mutable value object: has a setter, which violates immutability (CRITICAL: ddd:value-object-immutable). */
public class StockLevel {
    private int available;
    private int reserved;

    public StockLevel(int available, int reserved) {
        this.available = available;
        this.reserved = reserved;
    }

    public int getAvailable() { return available; }
    public int getReserved() { return reserved; }

    /** This setter violates value object immutability. */
    public void setAvailable(int available) { this.available = available; }
}
