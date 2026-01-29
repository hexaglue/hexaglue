package com.example.ecommerce.domain.customer;

import com.example.ecommerce.domain.shared.AggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Customer aggregate root representing a registered customer in the e-commerce platform.
 *
 * <p>A Customer is the central entity of the customer bounded context. It manages
 * personal information (name, email), multiple postal addresses, a loyalty program
 * with tiered benefits (0 to 3), and email verification status.
 *
 * <p>Business rules enforced by this aggregate:
 * <ul>
 *   <li>A customer must have a verified email before placing orders</li>
 *   <li>The first address added becomes both the default billing and shipping address</li>
 *   <li>Only addresses already in the address list can be set as defaults</li>
 *   <li>Loyalty tier ranges from 0 (standard) to 3 (platinum), with premium status at tier 2+</li>
 *   <li>Changing email resets the verification status</li>
 * </ul>
 *
 * <p>Domain events emitted: {@link CustomerCreatedEvent}, {@link CustomerAddressChangedEvent}.
 */
public class Customer extends AggregateRoot<CustomerId> {

    private final CustomerId id;
    private String firstName;
    private String lastName;
    private Email email;
    private final List<Address> addresses;
    private Address defaultBillingAddress;
    private Address defaultShippingAddress;
    private int loyaltyTier;
    private boolean emailVerified;
    private final Instant createdAt;
    private Instant updatedAt;

    private Customer(CustomerId id, String firstName, String lastName, Email email) {
        this.id = Objects.requireNonNull(id, "Customer ID cannot be null");
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.addresses = new ArrayList<>();
        this.loyaltyTier = 0;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Customer create(String firstName, String lastName, Email email) {
        Customer customer = new Customer(CustomerId.generate(), firstName, lastName, email);
        customer.registerEvent(new CustomerCreatedEvent(customer.id, firstName, lastName, email));
        return customer;
    }

    public static Customer reconstitute(CustomerId id, String firstName, String lastName,
                                        Email email, List<Address> addresses, int loyaltyTier) {
        Customer customer = new Customer(id, firstName, lastName, email);
        customer.addresses.addAll(addresses);
        customer.loyaltyTier = loyaltyTier;
        return customer;
    }

    @Override
    public CustomerId getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Email getEmail() {
        return email;
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public Address getDefaultBillingAddress() {
        return defaultBillingAddress;
    }

    public Address getDefaultShippingAddress() {
        return defaultShippingAddress;
    }

    public int getLoyaltyTier() {
        return loyaltyTier;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateName(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.updatedAt = Instant.now();
    }

    public void changeEmail(Email newEmail) {
        Objects.requireNonNull(newEmail, "Email cannot be null");
        this.email = newEmail;
        this.emailVerified = false;
        this.updatedAt = Instant.now();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    public void addAddress(Address address) {
        Objects.requireNonNull(address, "Address cannot be null");
        addresses.add(address);
        if (addresses.size() == 1) {
            this.defaultBillingAddress = address;
            this.defaultShippingAddress = address;
        }
        this.updatedAt = Instant.now();
    }

    public void setDefaultBillingAddress(Address address) {
        if (!addresses.contains(address)) {
            throw new IllegalArgumentException("Address must be in the address list");
        }
        Address oldAddress = this.defaultBillingAddress;
        this.defaultBillingAddress = address;
        this.updatedAt = Instant.now();
        registerEvent(new CustomerAddressChangedEvent(id, oldAddress, address));
    }

    public void setDefaultShippingAddress(Address address) {
        if (!addresses.contains(address)) {
            throw new IllegalArgumentException("Address must be in the address list");
        }
        this.defaultShippingAddress = address;
        this.updatedAt = Instant.now();
    }

    public void upgradeLoyaltyTier() {
        if (loyaltyTier < 3) {
            this.loyaltyTier++;
            this.updatedAt = Instant.now();
        }
    }

    public void downgradeLoyaltyTier() {
        if (loyaltyTier > 0) {
            this.loyaltyTier--;
            this.updatedAt = Instant.now();
        }
    }

    public boolean hasVerifiedEmail() {
        return emailVerified;
    }

    public boolean isPremiumCustomer() {
        return loyaltyTier >= 2;
    }
}
