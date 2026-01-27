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

package io.hexaglue.plugin.audit.domain.model.report;

/**
 * Total counts of all architectural elements.
 *
 * @param aggregates total aggregate roots
 * @param entities total entities
 * @param valueObjects total value objects
 * @param identifiers total identifier types
 * @param domainEvents total domain events
 * @param drivingPorts total driving ports
 * @param drivenPorts total driven ports
 * @since 5.0.0
 */
public record InventoryTotals(
        int aggregates,
        int entities,
        int valueObjects,
        int identifiers,
        int domainEvents,
        int drivingPorts,
        int drivenPorts) {

    /**
     * Returns the total number of domain types (aggregates + entities + value objects).
     *
     * @return total domain types
     */
    public int totalDomainTypes() {
        return aggregates + entities + valueObjects + identifiers + domainEvents;
    }

    /**
     * Returns the total number of ports (driving + driven).
     *
     * @return total ports
     */
    public int totalPorts() {
        return drivingPorts + drivenPorts;
    }

    /**
     * Returns the total number of all elements.
     *
     * @return total elements
     */
    public int total() {
        return totalDomainTypes() + totalPorts();
    }
}
