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

package io.hexaglue.plugin.audit.adapter.report.model;

import io.hexaglue.spi.ir.Port;
import java.util.Objects;

/**
 * An entry in the port matrix showing details of a single port.
 *
 * <p>The port matrix provides a tabular view of all ports in the application
 * for easy analysis of hexagonal architecture compliance.
 *
 * @param portName       the simple name of the port interface
 * @param qualifiedName  the fully qualified name of the port interface
 * @param direction      DRIVING or DRIVEN
 * @param kind           the port kind (REPOSITORY, GATEWAY, USE_CASE, etc.)
 * @param managedType    the primary domain type managed by this port (may be null)
 * @param methodCount    the number of methods defined in the port
 * @param hasAdapter     whether an adapter implementation exists for this port
 * @since 1.0.0
 */
public record PortMatrixEntry(
        String portName,
        String qualifiedName,
        String direction,
        String kind,
        String managedType,
        int methodCount,
        boolean hasAdapter) {

    public PortMatrixEntry {
        Objects.requireNonNull(portName, "portName required");
        Objects.requireNonNull(qualifiedName, "qualifiedName required");
        Objects.requireNonNull(direction, "direction required");
        Objects.requireNonNull(kind, "kind required");
        if (methodCount < 0) {
            throw new IllegalArgumentException("methodCount cannot be negative: " + methodCount);
        }
    }

    /**
     * Creates a PortMatrixEntry from a Port.
     *
     * @param port       the port to convert
     * @param hasAdapter whether this port has an adapter implementation
     * @return a new PortMatrixEntry
     */
    public static PortMatrixEntry from(Port port, boolean hasAdapter) {
        Objects.requireNonNull(port, "port required");
        return new PortMatrixEntry(
                port.simpleName(),
                port.qualifiedName(),
                port.direction().name(),
                port.kind().name(),
                port.primaryManagedType(),
                port.methods() != null ? port.methods().size() : 0,
                hasAdapter);
    }

    /**
     * Returns true if this is a driving (primary/inbound) port.
     *
     * @return true if direction is DRIVING
     */
    public boolean isDriving() {
        return "DRIVING".equals(direction);
    }

    /**
     * Returns true if this is a driven (secondary/outbound) port.
     *
     * @return true if direction is DRIVEN
     */
    public boolean isDriven() {
        return "DRIVEN".equals(direction);
    }
}
