package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.AggregateRoot;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.inventory.InventoryItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root representing a customer order.
 *
 * AUDIT VIOLATION: ddd:aggregate-cycle
 * This aggregate has a direct reference to InventoryItem from another aggregate,
 * creating a potential cycle between Order and Inventory aggregates.
 */
public class Order extends AggregateRoot<OrderId> {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String shippingTrackingNumber;
    private String cancellationReason;

    // VIOLATION: Direct reference to another aggregate's entity
    private InventoryItem reservedInventory;

    private Order(OrderId id, CustomerId customerId) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.lines = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Order create(CustomerId customerId) {
        return new Order(OrderId.generate(), customerId);
    }

    public static Order reconstitute(OrderId id, CustomerId customerId, OrderStatus status,
                                     List<OrderLine> lines, Instant createdAt) {
        Order order = new Order(id, customerId);
        order.status = status;
        order.lines.addAll(lines);
        return order;
    }

    @Override
    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getShippingTrackingNumber() {
        return shippingTrackingNumber;
    }

    /**
     * VIOLATION: Setter for aggregate reference creates coupling
     */
    public void setReservedInventory(InventoryItem inventory) {
        this.reservedInventory = inventory;
    }

    public InventoryItem getReservedInventory() {
        return reservedInventory;
    }

    public void addLine(OrderLine line) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot add lines to a non-draft order");
        }
        lines.add(Objects.requireNonNull(line, "Order line cannot be null"));
        this.updatedAt = Instant.now();
    }

    public void removeLine(int index) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove lines from a non-draft order");
        }
        if (index < 0 || index >= lines.size()) {
            throw new IndexOutOfBoundsException("Invalid line index: " + index);
        }
        lines.remove(index);
        this.updatedAt = Instant.now();
    }

    public Money getTotalAmount() {
        if (lines.isEmpty()) {
            return Money.zero(Currency.getInstance("EUR"));
        }
        return lines.stream()
                .map(OrderLine::getTotalPrice)
                .reduce(Money::add)
                .orElse(Money.zero(Currency.getInstance("EUR")));
    }

    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Can only place draft orders");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place an empty order");
        }
        this.status = OrderStatus.PENDING_PAYMENT;
        this.updatedAt = Instant.now();
        registerEvent(new OrderPlaceEvent(id, customerId, getTotalAmount()));
    }

    public void confirmPayment() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Can only confirm payment for pending orders");
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void startProcessing() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Can only process paid orders");
        }
        this.status = OrderStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void ship(String trackingNumber, String carrier) {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Can only ship processing orders");
        }
        this.shippingTrackingNumber = trackingNumber;
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
        registerEvent(new OrderShippedEvent(id, trackingNumber, carrier));
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Can only deliver shipped orders");
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (!status.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        this.cancellationReason = reason;
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
        registerEvent(new OrderCancelEvent(id, reason));
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public boolean isDraft() {
        return status == OrderStatus.DRAFT;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public int getLineCount() {
        return lines.size();
    }
}
