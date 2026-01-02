package com.ecommerce.domain.customer;

import com.ecommerce.domain.order.Address;
import java.time.Instant;

/**
 * Customer aggregate root.
 */
public class Customer {

    private final CustomerId id;
    private String firstName;
    private String lastName;
    private Email email;
    private Address billingAddress;
    private final Instant createdAt;

    public Customer(CustomerId id, String firstName, String lastName, Email email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.createdAt = Instant.now();
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

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Email getEmail() {
        return email;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(String firstName, String lastName, Email email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public void setBillingAddress(Address address) {
        this.billingAddress = address;
    }
}
