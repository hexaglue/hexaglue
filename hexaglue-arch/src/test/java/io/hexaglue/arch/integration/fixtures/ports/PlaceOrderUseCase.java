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

import io.hexaglue.arch.integration.fixtures.domain.Address;
import io.hexaglue.arch.integration.fixtures.domain.CustomerId;
import io.hexaglue.arch.integration.fixtures.domain.Money;
import io.hexaglue.arch.integration.fixtures.domain.OrderId;
import java.util.List;

/**
 * Use case for placing an order (driving port).
 */
@DrivingPort
public interface PlaceOrderUseCase {

    /**
     * Places a new order.
     *
     * @param customerId the customer placing the order
     * @param lines the order line items
     * @param shippingAddress the shipping address
     * @return the created order ID
     */
    OrderId execute(CustomerId customerId, List<LineItem> lines, Address shippingAddress);

    /**
     * Line item DTO for the use case.
     */
    record LineItem(String productId, int quantity, Money unitPrice) {}
}
