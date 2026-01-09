package com.example.ecommerce.domain.model;

import java.time.Instant;

/**
 * Aggregate Root representing a Customer in the e-commerce domain.
 */
public class Customer {
    private final CustomerId id;
    private String firstName;
    private String lastName;
    private Email email;
    private PhoneNumber phoneNumber;
    private Address defaultShippingAddress;
    private final Instant createdAt;
    private Instant updatedAt;

    public Customer(
        CustomerId id,
        String firstName,
        String lastName,
        Email email,
        PhoneNumber phoneNumber,
        Address defaultShippingAddress
    ) {
        if (id == null) {
            throw new IllegalArgumentException("CustomerId cannot be null");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be null or blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be null or blank");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.defaultShippingAddress = defaultShippingAddress;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateContactInfo(Email email, PhoneNumber phoneNumber) {
        if (email != null) {
            this.email = email;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        this.updatedAt = Instant.now();
    }

    public void updateDefaultShippingAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        this.defaultShippingAddress = address;
        this.updatedAt = Instant.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public CustomerId getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Email getEmail() {
        return email;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public Address getDefaultShippingAddress() {
        return defaultShippingAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
