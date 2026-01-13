package com.example.validation.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jmolecules.ddd.annotation.AggregateRoot;

/**
 * Order aggregate root.
 *
 * <p>Classification: EXPLICIT via @AggregateRoot annotation.
 *
 * <p>This demonstrates the highest confidence classification - explicit annotations
 * from jMolecules are always trusted (Priority 100).
 */
@AggregateRoot
public class Order {

    private final OrderId id;
    private final String customerId;
    private final List<OrderLine> lines;
    private final LocalDateTime createdAt;
    private OrderStatus status;

    public Order(OrderId id, String customerId) {
        this.id = id;
        this.customerId = customerId;
        this.lines = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.DRAFT;
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addLine(String productId, int quantity, BigDecimal unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a confirmed order");
        }
        lines.add(new OrderLine(productId, quantity, unitPrice));
    }

    public void confirm() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public BigDecimal getTotal() {
        return lines.stream()
                .map(OrderLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public enum OrderStatus {
        DRAFT,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
