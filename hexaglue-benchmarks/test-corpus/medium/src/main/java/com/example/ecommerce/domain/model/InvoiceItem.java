package com.example.ecommerce.domain.model;

/**
 * Entity representing An item in an invoice.
 */
public class InvoiceItem {
    private final String id;
    private String name;

    public InvoiceItem(String id, String name) {
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
