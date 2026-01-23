package com.regression.ports.out;

import com.regression.domain.customer.CustomerId;
import com.regression.domain.order.Order;
import com.regression.domain.order.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (Repository) for Order persistence.
 * <p>
 * Tests multiple corrections:
 * - C1: getById, loadById, fetchById should all be recognized as FIND_BY_ID
 *       and generate working implementations (not UnsupportedOperationException)
 * - C4: Repository signatures should use UUID for identifier parameters
 *       in the generated JPA repository (OrderId → UUID)
 * - M7: Generic return types should be fully displayed
 */
public interface OrderRepository {

    /**
     * Saves an order.
     */
    Order save(Order order);

    /**
     * Finds an order by ID using standard "findById" naming.
     */
    Optional<Order> findById(OrderId id);

    /**
     * Gets an order by ID.
     * <p>
     * Tests C1: This method name variant should work correctly.
     */
    Optional<Order> getById(OrderId id);

    /**
     * Loads an order by ID.
     * <p>
     * Tests C1: This method name variant should work correctly.
     */
    Optional<Order> loadById(OrderId id);

    /**
     * Fetches an order by ID.
     * <p>
     * Tests C1: This method name variant should work correctly.
     */
    Optional<Order> fetchById(OrderId id);

    /**
     * Finds all orders for a specific customer.
     * <p>
     * Tests C4: CustomerId parameter should be converted to UUID
     * when calling the JPA repository.
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Finds all urgent orders.
     */
    List<Order> findByUrgent(boolean urgent);

    /**
     * Checks if any order exists for a customer.
     * <p>
     * Tests M12: boolean return type with Identifier parameter.
     */
    boolean existsByCustomerId(CustomerId customerId);

    /**
     * Lists all orders.
     */
    List<Order> findAll();

    /**
     * Deletes an order.
     */
    void delete(Order order);
}
