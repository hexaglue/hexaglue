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
