/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.arch.integration.fixtures.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order aggregate root.
 *
 * <p>Represents a customer order with line items, shipping address, and total.</p>
 */
@AggregateRoot
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private Address shippingAddress;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(OrderId id, CustomerId customerId) {
        this.id = id;
        this.customerId = customerId;
        this.lines = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public void addLine(String productId, int quantity, Money unitPrice) {
        lines.add(new OrderLine(productId, quantity, unitPrice));
    }

    public void setShippingAddress(Address address) {
        this.shippingAddress = address;
    }

    public void place() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place empty order");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Shipping address required");
        }
        this.status = OrderStatus.PLACED;
    }

    public Money total() {
        return lines.stream().map(OrderLine::subtotal).reduce(Money.euros(java.math.BigDecimal.ZERO), Money::add);
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public OrderStatus getStatus() {
        return status;
    }
}
