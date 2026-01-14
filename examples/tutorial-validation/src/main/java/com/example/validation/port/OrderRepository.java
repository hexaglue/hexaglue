package com.example.validation.port;

import com.example.validation.domain.Order;
import com.example.validation.domain.OrderId;
import java.util.List;
import java.util.Optional;
import org.jmolecules.ddd.annotation.Repository;

/**
 * Order repository port (driven/secondary).
 *
 * <p>Classification: DRIVEN port via @Repository annotation and semantic analysis.
 *
 * <p>This interface represents the persistence port for Order aggregates.
 * It follows the repository pattern from DDD.
 */
@Repository
public interface OrderRepository {

    /**
     * Saves an order to the repository.
     *
     * @param order the order to save
     * @return the saved order
     */
    Order save(Order order);

    /**
     * Finds an order by its identifier.
     *
     * @param id the order identifier
     * @return the order if found
     */
    Optional<Order> findById(OrderId id);

    /**
     * Finds all orders for a customer.
     *
     * @param customerId the customer identifier
     * @return list of orders for the customer
     */
    List<Order> findByCustomerId(String customerId);

    /**
     * Deletes an order by its identifier.
     *
     * @param id the order identifier
     */
    void deleteById(OrderId id);

    /**
     * Checks if an order exists.
     *
     * @param id the order identifier
     * @return true if the order exists
     */
    boolean existsById(OrderId id);
}
