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

import io.hexaglue.arch.integration.fixtures.domain.Money;
import io.hexaglue.arch.integration.fixtures.domain.OrderId;

/**
 * Gateway to external payment system (driven port).
 */
@DrivenPort
public interface PaymentGateway {

    /**
     * Charges a payment for an order.
     *
     * @param orderId the order ID
     * @param amount the amount to charge
     * @return the payment reference
     */
    String charge(OrderId orderId, Money amount);

    /**
     * Refunds a payment.
     *
     * @param paymentReference the payment reference to refund
     * @return true if refund succeeded
     */
    boolean refund(String paymentReference);
}
