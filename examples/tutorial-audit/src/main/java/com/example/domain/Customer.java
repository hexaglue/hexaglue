package com.example.domain;

/**
 * Customer aggregate root.
 *
 * Demonstrates best practices:
 * - Has identity (CustomerId)
 * - Uses immutable value objects (Email, Address)
 * - No framework dependencies
 * - Repository exists (CustomerRepository)
 */
public class Customer {
    private final CustomerId id;
    private String name;
    private Email email;
    private Address shippingAddress;

    public Customer(CustomerId id, String name, Email email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public CustomerId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.name = newName;
    }

    public void updateEmail(Email newEmail) {
        this.email = newEmail;
    }

    public void setShippingAddress(Address address) {
        this.shippingAddress = address;
    }
}
