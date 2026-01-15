package io.hexaglue.arch.integration.fixtures.ports;

import io.hexaglue.arch.integration.fixtures.domain.Order;
import io.hexaglue.arch.integration.fixtures.domain.OrderId;
import java.util.Optional;

/**
 * Repository for Order aggregate (driven port).
 *
 * <p>This interface uses the *Repository naming convention,
 * so it should be classified as DRIVEN_PORT even without
 * explicit annotation.</p>
 */
public interface OrderRepository {

    /**
     * Finds an order by its ID.
     *
     * @param id the order ID
     * @return the order if found
     */
    Optional<Order> findById(OrderId id);

    /**
     * Saves an order.
     *
     * @param order the order to save
     */
    void save(Order order);

    /**
     * Deletes an order.
     *
     * @param id the order ID
     */
    void delete(OrderId id);
}
