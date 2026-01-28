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
import java.util.Objects;
import java.util.Optional;

/**
 * Details about a port component (driving or driven).
 *
 * @param name simple name of the port
 * @param packageName fully qualified package name
 * @param kind port kind (REPOSITORY, GATEWAY, etc.) - null for driving ports
 * @param methods number of methods
 * @param hasAdapter whether the port has an adapter implementation
 * @param adapter name of the adapter if present
 * @param orchestrates aggregates orchestrated by this port (for driving ports)
 * @param methodDetails detailed information about port methods for diagram rendering
 * @since 5.0.0
 */
public record PortComponent(
        String name,
        String packageName,
        String kind,
        int methods,
        boolean hasAdapter,
        String adapter,
        List<String> orchestrates,
        List<MethodDetail> methodDetails) {

    /**
     * Creates a port component with validation.
     */
    public PortComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        orchestrates = orchestrates != null ? List.copyOf(orchestrates) : List.of();
        methodDetails = methodDetails != null ? List.copyOf(methodDetails) : List.of();
    }

    /**
     * Creates a driving port component.
     *
     * @param name port name
     * @param packageName package name
     * @param methods number of methods
     * @param hasAdapter whether it has an adapter
     * @param adapter adapter name
     * @param orchestrates aggregates it orchestrates
     * @return the port component
     */
    public static PortComponent driving(
            String name,
            String packageName,
            int methods,
            boolean hasAdapter,
            String adapter,
            List<String> orchestrates) {
        return new PortComponent(name, packageName, null, methods, hasAdapter, adapter, orchestrates, List.of());
    }

    /**
     * Creates a driving port component with method details.
     *
     * @param name port name
     * @param packageName package name
     * @param methods number of methods
     * @param hasAdapter whether it has an adapter
     * @param adapter adapter name
     * @param orchestrates aggregates it orchestrates
     * @param methodDetails method details for rendering
     * @return the port component
     */
    public static PortComponent driving(
            String name,
            String packageName,
            int methods,
            boolean hasAdapter,
            String adapter,
            List<String> orchestrates,
            List<MethodDetail> methodDetails) {
        return new PortComponent(name, packageName, null, methods, hasAdapter, adapter, orchestrates, methodDetails);
    }

    /**
     * Creates a driven port component.
     *
     * @param name port name
     * @param packageName package name
     * @param kind port kind
     * @param methods number of methods
     * @param hasAdapter whether it has an adapter
     * @param adapter adapter name
     * @return the port component
     */
    public static PortComponent driven(
            String name, String packageName, String kind, int methods, boolean hasAdapter, String adapter) {
        return new PortComponent(name, packageName, kind, methods, hasAdapter, adapter, List.of(), List.of());
    }

    /**
     * Creates a driven port component with method details.
     *
     * @param name port name
     * @param packageName package name
     * @param kind port kind
     * @param methods number of methods
     * @param hasAdapter whether it has an adapter
     * @param adapter adapter name
     * @param methodDetails method details for rendering
     * @return the port component
     */
    public static PortComponent driven(
            String name,
            String packageName,
            String kind,
            int methods,
            boolean hasAdapter,
            String adapter,
            List<MethodDetail> methodDetails) {
        return new PortComponent(name, packageName, kind, methods, hasAdapter, adapter, List.of(), methodDetails);
    }

    /**
     * Returns the adapter name as optional.
     *
     * @return optional adapter name
     */
    public Optional<String> adapterOpt() {
        return Optional.ofNullable(adapter);
    }

    /**
     * Returns the kind as optional.
     *
     * @return optional kind
     */
    public Optional<String> kindOpt() {
        return Optional.ofNullable(kind);
    }
}
