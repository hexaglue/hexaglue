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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.index.PortIndex;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
 * @param adapterStatus  the adapter detection status: "DETECTED", "NOT_DETECTED", or "UNKNOWN"
 * @since 1.0.0
 * @since 5.0.0 - Changed hasAdapter (boolean) to adapterStatus (String) for honest reporting
 */
public record PortMatrixEntry(
        String portName,
        String qualifiedName,
        String direction,
        String kind,
        String managedType,
        int methodCount,
        String adapterStatus) {

    /**
     * Adapter status indicating the adapter has been detected.
     */
    public static final String ADAPTER_DETECTED = "DETECTED";

    /**
     * Adapter status indicating no adapter was detected (but might exist).
     */
    public static final String ADAPTER_NOT_DETECTED = "NOT_DETECTED";

    /**
     * Adapter status indicating detection was not performed.
     */
    public static final String ADAPTER_UNKNOWN = "UNKNOWN";

    public PortMatrixEntry {
        Objects.requireNonNull(portName, "portName required");
        Objects.requireNonNull(qualifiedName, "qualifiedName required");
        Objects.requireNonNull(direction, "direction required");
        Objects.requireNonNull(kind, "kind required");
        Objects.requireNonNull(adapterStatus, "adapterStatus required");
        if (methodCount < 0) {
            throw new IllegalArgumentException("methodCount cannot be negative: " + methodCount);
        }
    }

    /**
     * Returns true if an adapter was detected for this port.
     *
     * @return true if adapterStatus is DETECTED
     */
    public boolean hasAdapter() {
        return ADAPTER_DETECTED.equals(adapterStatus);
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

    /**
     * Creates a list of PortMatrixEntry from an ArchitecturalModel.
     *
     * <p>This method extracts all driving and driven ports from the model
     * and converts them to PortMatrixEntry records for the port matrix report.
     *
     * @param model the architectural model
     * @return list of port matrix entries
     * @since 4.0.0
     */
    /**
     * Creates a list of PortMatrixEntry from an ArchitecturalModel.
     *
     * <p>This method extracts all driving and driven ports from the model
     * and converts them to PortMatrixEntry records for the port matrix report.
     *
     * @param model the architectural model
     * @return list of port matrix entries
     * @since 4.0.0
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     * @since 5.0.0 - Migrated to v5 ArchType API with PortIndex
     */
    public static List<PortMatrixEntry> fromModel(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model required");

        PortIndex portIndex = model.portIndex().orElseThrow();
        Stream<PortMatrixEntry> drivingEntries = portIndex.drivingPorts().map(PortMatrixEntry::fromDrivingPort);
        Stream<PortMatrixEntry> drivenEntries = portIndex.drivenPorts().map(PortMatrixEntry::fromDrivenPort);

        return Stream.concat(drivingEntries, drivenEntries).toList();
    }

    /**
     * Creates a PortMatrixEntry from a DrivingPort.
     */
    private static PortMatrixEntry fromDrivingPort(DrivingPort port) {
        return new PortMatrixEntry(
                port.id().simpleName(),
                port.id().qualifiedName(),
                "DRIVING",
                "USE_CASE", // Default kind for driving ports
                null, // managedType not available in v5 model
                port.structure().methods().size(),
                ADAPTER_NOT_DETECTED); // Adapter detection requires plugin coordination (see FUTURE_ADAPTER_DETECTION.md)
    }

    /**
     * Creates a PortMatrixEntry from a DrivenPort.
     */
    private static PortMatrixEntry fromDrivenPort(DrivenPort port) {
        String kind = port.portType().name();
        String managedType = port.managedAggregate()
                .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                .orElse(null);
        return new PortMatrixEntry(
                port.id().simpleName(),
                port.id().qualifiedName(),
                "DRIVEN",
                kind,
                managedType,
                port.structure().methods().size(),
                ADAPTER_NOT_DETECTED); // Adapter detection requires plugin coordination (see FUTURE_ADAPTER_DETECTION.md)
    }
}
