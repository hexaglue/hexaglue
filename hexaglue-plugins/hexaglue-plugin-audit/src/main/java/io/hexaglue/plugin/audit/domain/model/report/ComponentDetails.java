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

import java.util.List;

/**
 * Detailed breakdown of all architectural components.
 *
 * @param aggregates list of aggregate root components
 * @param valueObjects list of value object components
 * @param identifiers list of identifier components
 * @param drivingPorts list of driving port components
 * @param drivenPorts list of driven port components
 * @param adapters list of adapter components
 * @since 5.0.0
 */
public record ComponentDetails(
        List<AggregateComponent> aggregates,
        List<ValueObjectComponent> valueObjects,
        List<IdentifierComponent> identifiers,
        List<PortComponent> drivingPorts,
        List<PortComponent> drivenPorts,
        List<AdapterComponent> adapters) {

    /**
     * Creates component details with defensive copies.
     */
    public ComponentDetails {
        aggregates = aggregates != null ? List.copyOf(aggregates) : List.of();
        valueObjects = valueObjects != null ? List.copyOf(valueObjects) : List.of();
        identifiers = identifiers != null ? List.copyOf(identifiers) : List.of();
        drivingPorts = drivingPorts != null ? List.copyOf(drivingPorts) : List.of();
        drivenPorts = drivenPorts != null ? List.copyOf(drivenPorts) : List.of();
        adapters = adapters != null ? List.copyOf(adapters) : List.of();
    }

    /**
     * Creates an empty component details instance.
     *
     * @return empty component details
     */
    public static ComponentDetails empty() {
        return new ComponentDetails(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
