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

package io.hexaglue.arch.model;

/**
 * Types of driven (secondary/outbound) ports in hexagonal architecture.
 *
 * <p>Driven ports are interfaces that the application uses to interact with
 * external systems. They represent the "what" a port does, independent of
 * the port direction (which is always DRIVEN for these types).</p>
 *
 * <h2>Mapping from PortKind (in hexaglue-core)</h2>
 * <p>The conversion from {@code PortKind} to {@code DrivenPortType} is performed
 * in the {@code ArchitecturalModelBuilder} (hexaglue-core) module:</p>
 * <table>
 *   <tr><th>PortKind</th><th>DrivenPortType</th></tr>
 *   <tr><td>REPOSITORY</td><td>REPOSITORY</td></tr>
 *   <tr><td>GATEWAY</td><td>GATEWAY</td></tr>
 *   <tr><td>EVENT_PUBLISHER</td><td>EVENT_PUBLISHER</td></tr>
 *   <tr><td>GENERIC</td><td>OTHER</td></tr>
 *   <tr><td>USE_CASE, QUERY, COMMAND</td><td>OTHER (driving ports)</td></tr>
 * </table>
 *
 * @since 4.1.0
 */
public enum DrivenPortType {

    /**
     * Repository port - persistence abstraction for aggregates.
     *
     * <p>Manages the lifecycle and persistence of aggregate roots.</p>
     */
    REPOSITORY("Persistence abstraction for aggregates"),

    /**
     * Gateway port - external service abstraction.
     *
     * <p>Abstracts interactions with external systems like APIs, messaging, etc.</p>
     */
    GATEWAY("External service abstraction"),

    /**
     * Event publisher port - publishes domain events.
     *
     * <p>Abstracts the mechanism for publishing domain events to external consumers.</p>
     */
    EVENT_PUBLISHER("Publishes domain events"),

    /**
     * Notification port - sends notifications.
     *
     * <p>Abstracts the mechanism for sending notifications (email, SMS, push, etc.).</p>
     */
    NOTIFICATION("Sends notifications"),

    /**
     * Other driven port - could not be classified more specifically.
     *
     * <p>Used for driven ports that don't fit the standard categories.</p>
     */
    OTHER("Generic driven port");

    private final String description;

    DrivenPortType(String description) {
        this.description = description;
    }

    /**
     * Returns a human-readable description of this port type.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
