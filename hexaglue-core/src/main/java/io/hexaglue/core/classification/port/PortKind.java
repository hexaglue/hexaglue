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
 * Classification kinds for port interfaces in hexagonal architecture.
 */
public enum PortKind {

    /**
     * Repository port - persistence abstraction for aggregates.
     * Direction: DRIVEN (outbound/secondary).
     */
    REPOSITORY,

    /**
     * Use case port - application service / command handler.
     * Direction: DRIVING (inbound/primary).
     */
    USE_CASE,

    /**
     * Gateway port - external service abstraction.
     * Direction: DRIVEN (outbound/secondary).
     */
    GATEWAY,

    /**
     * Query port - read-only data access.
     * Direction: DRIVING (inbound/primary).
     */
    QUERY,

    /**
     * Command port - write operations / state changes.
     * Direction: DRIVING (inbound/primary).
     */
    COMMAND,

    /**
     * Event publisher port - publishes domain events.
     * Direction: DRIVEN (outbound/secondary).
     */
    EVENT_PUBLISHER,

    /**
     * Generic port - could not be classified more specifically.
     */
    GENERIC
}
