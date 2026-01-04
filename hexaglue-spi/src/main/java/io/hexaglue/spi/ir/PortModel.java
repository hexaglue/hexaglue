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

import java.util.List;
import java.util.Optional;

/**
 * The port model extracted from the application.
 *
 * @param ports all detected ports (repositories, gateways, use cases, etc.)
 */
public record PortModel(List<Port> ports) {

    /**
     * Finds a port by its qualified name.
     */
    public Optional<Port> findByQualifiedName(String qualifiedName) {
        return ports.stream()
                .filter(p -> p.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    /**
     * Returns all ports of a specific kind.
     */
    public List<Port> portsOfKind(PortKind kind) {
        return ports.stream().filter(p -> p.kind() == kind).toList();
    }

    /**
     * Returns all driving ports (primary/inbound).
     */
    public List<Port> drivingPorts() {
        return ports.stream()
                .filter(p -> p.direction() == PortDirection.DRIVING)
                .toList();
    }

    /**
     * Returns all driven ports (secondary/outbound).
     */
    public List<Port> drivenPorts() {
        return ports.stream().filter(p -> p.direction() == PortDirection.DRIVEN).toList();
    }

    /**
     * Returns all repositories.
     */
    public List<Port> repositories() {
        return portsOfKind(PortKind.REPOSITORY);
    }
}
