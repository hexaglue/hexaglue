package com.example.ecommerce.infrastructure.persistence;

import com.example.ecommerce.application.port.driven.OrderRepository;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.order.Order;
import com.example.ecommerce.domain.order.OrderId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA-based driven adapter implementing the {@link OrderRepository} port.
 *
 * <p>This adapter provides order persistence using JPA's EntityManager for
 * database operations. In this demonstration, an in-memory HashMap supplements
 * the JPA storage. It also exposes JPA-specific methods such as {@code flush()}
 * and {@code clear()} for fine-grained persistence context management.
 *
 * <p>Note: Infrastructure adapters are allowed to use JPA annotations as they
 * reside in the infrastructure layer, outside the domain boundary.
 */
public class JpaOrderRepository implements OrderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // In-memory storage for demo (would use EntityManager in real implementation)
    private final Map<OrderId, Order> storage = new HashMap<>();

    @Override
    public void save(Order order) {
        storage.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return storage.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(OrderId id) {
        storage.remove(id);
    }

    @Override
    public boolean exists(OrderId id) {
        return storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    /**
     * JPA-specific method for flushing changes.
     * This method is used by OrderApplicationService (violation).
     */
    public void flush() {
        if (entityManager != null) {
            entityManager.flush();
        }
    }

    /**
     * JPA-specific method for clearing the persistence context.
     */
    public void clear() {
        if (entityManager != null) {
            entityManager.clear();
        }
    }
}
