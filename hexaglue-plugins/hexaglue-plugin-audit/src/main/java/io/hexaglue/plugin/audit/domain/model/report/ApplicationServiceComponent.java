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

/**
 * Details about an application service component.
 *
 * <p>An application service orchestrates use cases by coordinating between
 * the domain layer and external systems through ports. It implements driving
 * ports and uses driven ports to accomplish business operations.
 *
 * @param name simple name of the application service
 * @param packageName fully qualified package name
 * @param methods number of methods
 * @param orchestrates aggregates orchestrated by this service
 * @param usesPorts ports used by this service
 * @param methodDetails detailed information about public methods for diagram rendering
 * @since 5.0.0
 */
public record ApplicationServiceComponent(
        String name,
        String packageName,
        int methods,
        List<String> orchestrates,
        List<String> usesPorts,
        List<MethodDetail> methodDetails) {

    /**
     * Creates an application service component with validation.
     */
    public ApplicationServiceComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        orchestrates = orchestrates != null ? List.copyOf(orchestrates) : List.of();
        usesPorts = usesPorts != null ? List.copyOf(usesPorts) : List.of();
        methodDetails = methodDetails != null ? List.copyOf(methodDetails) : List.of();
    }

    /**
     * Creates an application service component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param methods number of methods
     * @param orchestrates aggregates orchestrated
     * @param usesPorts ports used
     * @return the application service component
     */
    public static ApplicationServiceComponent of(
            String name, String packageName, int methods, List<String> orchestrates, List<String> usesPorts) {
        return new ApplicationServiceComponent(name, packageName, methods, orchestrates, usesPorts, List.of());
    }

    /**
     * Creates an application service component with method details.
     *
     * @param name simple name
     * @param packageName package name
     * @param methods number of methods
     * @param orchestrates aggregates orchestrated
     * @param usesPorts ports used
     * @param methodDetails method details for rendering
     * @return the application service component
     */
    public static ApplicationServiceComponent of(
            String name,
            String packageName,
            int methods,
            List<String> orchestrates,
            List<String> usesPorts,
            List<MethodDetail> methodDetails) {
        return new ApplicationServiceComponent(name, packageName, methods, orchestrates, usesPorts, methodDetails);
    }

    /**
     * Creates an application service component without orchestration info.
     *
     * @param name simple name
     * @param packageName package name
     * @param methods number of methods
     * @return the application service component
     */
    public static ApplicationServiceComponent of(String name, String packageName, int methods) {
        return new ApplicationServiceComponent(name, packageName, methods, List.of(), List.of(), List.of());
    }
}
