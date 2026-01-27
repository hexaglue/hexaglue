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

package io.hexaglue.arch.model.ir;

/**
 * Classification of port interfaces.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.ir
 */
public enum PortKind {

    /**
     * Repository - persistence abstraction for aggregates.
     */
    REPOSITORY,

    /**
     * Gateway - abstraction for external system integration.
     */
    GATEWAY,

    /**
     * Use case - application service defining a business operation.
     */
    USE_CASE,

    /**
     * Command handler - handles a specific command.
     */
    COMMAND,

    /**
     * Query handler - handles a specific query.
     */
    QUERY,

    /**
     * Event publisher - publishes domain events.
     */
    EVENT_PUBLISHER,

    /**
     * Generic port - could not be classified more specifically.
     */
    GENERIC
}
