package com.regression.domain.customer;

/**
 * Customer aggregate root.
 */
public record Customer(
        CustomerId id,
        String name,
        Email email,
        boolean active) {

    public Customer {
        if (id == null) {
            throw new IllegalArgumentException("Customer id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be null or blank");
        }
        if (email == null) {
            throw new IllegalArgumentException("Customer email cannot be null");
        }
    }

    public static Customer create(String name, Email email) {
        return new Customer(CustomerId.generate(), name, email, true);
    }

    public Customer deactivate() {
        return new Customer(id, name, email, false);
    }

    public Customer activate() {
        return new Customer(id, name, email, true);
    }

    public Customer changeName(String newName) {
        return new Customer(id, newName, email, active);
    }

    public Customer changeEmail(Email newEmail) {
        return new Customer(id, name, newEmail, active);
    }
}
