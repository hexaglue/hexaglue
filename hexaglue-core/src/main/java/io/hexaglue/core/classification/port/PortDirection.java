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

package io.hexaglue.core.classification.port;

/**
 * Direction of a port in hexagonal architecture.
 */
public enum PortDirection {

    /**
     * Driving port (inbound/primary) - the outside world drives the application.
     * Examples: REST controllers call use cases, CLI commands.
     */
    DRIVING,

    /**
     * Driven port (outbound/secondary) - the application drives external services.
     * Examples: Repositories, gateways, notification services.
     */
    DRIVEN
}
