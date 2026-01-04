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

package io.hexaglue.spi.ir;

/**
 * Direction of a port in hexagonal architecture.
 */
public enum PortDirection {

    /**
     * Driving port (primary/inbound).
     *
     * <p>These ports are called BY external actors (UI, REST controllers, etc.)
     * to drive the application. Examples: use cases, command handlers.
     */
    DRIVING,

    /**
     * Driven port (secondary/outbound).
     *
     * <p>These ports are called BY the application to interact with
     * external systems. Examples: repositories, gateways, event publishers.
     */
    DRIVEN
}
