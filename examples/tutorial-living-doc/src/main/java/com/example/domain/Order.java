package com.example.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order aggregate root.
 * Manages order lines and calculates totals.
 */
public class Order {
    private final OrderId id;
    private final String customerName;
    private final List<OrderLine> lines;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(OrderId id, String customerName) {
        this.id = id;
        this.customerName = customerName;
        this.lines = new ArrayList<>();
        this.createdAt = Instant.now();
        this.status = OrderStatus.DRAFT;
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addLine(OrderLine line) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a confirmed order");
        }
        this.lines.add(line);
    }

    public Money getTotal() {
        return lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.euros(java.math.BigDecimal.ZERO), Money::add);
    }

    public void confirm() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}
