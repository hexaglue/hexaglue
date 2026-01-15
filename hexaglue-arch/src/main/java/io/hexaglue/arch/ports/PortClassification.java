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

package io.hexaglue.arch.ports;

/**
 * Classification of port types in Hexagonal Architecture.
 *
 * <h2>Driving Ports (Primary/Inbound)</h2>
 * <ul>
 *   <li>{@link #USE_CASE} - Application use case interface</li>
 *   <li>{@link #COMMAND_HANDLER} - CQRS command handler</li>
 *   <li>{@link #QUERY_HANDLER} - CQRS query handler</li>
 * </ul>
 *
 * <h2>Driven Ports (Secondary/Outbound)</h2>
 * <ul>
 *   <li>{@link #REPOSITORY} - Aggregate persistence</li>
 *   <li>{@link #GATEWAY} - External system integration</li>
 *   <li>{@link #EVENT_PUBLISHER} - Domain event publishing</li>
 *   <li>{@link #NOTIFICATION} - External notification service</li>
 * </ul>
 *
 * @since 4.0.0
 */
public enum PortClassification {

    // === Driving Ports ===

    /**
     * Use case interface defining an application behavior.
     */
    USE_CASE,

    /**
     * CQRS command handler.
     */
    COMMAND_HANDLER,

    /**
     * CQRS query handler.
     */
    QUERY_HANDLER,

    // === Driven Ports ===

    /**
     * Repository for aggregate persistence.
     */
    REPOSITORY,

    /**
     * Gateway to external systems.
     */
    GATEWAY,

    /**
     * Domain event publisher.
     */
    EVENT_PUBLISHER,

    /**
     * External notification service.
     */
    NOTIFICATION,

    /**
     * Unclassified port type.
     */
    UNKNOWN;

    /**
     * Returns whether this is a driving (inbound) port classification.
     *
     * @return true if driving port
     */
    public boolean isDriving() {
        return this == USE_CASE || this == COMMAND_HANDLER || this == QUERY_HANDLER;
    }

    /**
     * Returns whether this is a driven (outbound) port classification.
     *
     * @return true if driven port
     */
    public boolean isDriven() {
        return this == REPOSITORY || this == GATEWAY || this == EVENT_PUBLISHER || this == NOTIFICATION;
    }
}
