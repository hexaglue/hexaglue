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

/**
 * Domain event raised when an order is placed.
 */
@DomainEvent
public record OrderPlaced(OrderId orderId, CustomerId customerId, Money total, Instant occurredAt) {

    public static OrderPlaced of(OrderId orderId, CustomerId customerId, Money total) {
        return new OrderPlaced(orderId, customerId, total, Instant.now());
    }
}
