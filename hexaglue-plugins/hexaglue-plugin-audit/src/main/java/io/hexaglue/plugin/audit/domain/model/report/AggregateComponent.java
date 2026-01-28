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
 * Details about an aggregate root component.
 *
 * <p>Contains both summary information (field count) and detailed information
 * (individual field and method details) for rendering in diagrams.
 *
 * @param name simple name of the aggregate
 * @param packageName fully qualified package name
 * @param fields number of fields
 * @param references other aggregates this one references
 * @param usesPorts ports used by this aggregate
 * @param fieldDetails detailed information about each field for diagram rendering
 * @param methodDetails detailed information about public business methods for diagram rendering
 * @since 5.0.0
 */
public record AggregateComponent(
        String name,
        String packageName,
        int fields,
        List<String> references,
        List<String> usesPorts,
        List<FieldDetail> fieldDetails,
        List<MethodDetail> methodDetails) {

    /**
     * Creates an aggregate component with validation.
     */
    public AggregateComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        references = references != null ? List.copyOf(references) : List.of();
        usesPorts = usesPorts != null ? List.copyOf(usesPorts) : List.of();
        fieldDetails = fieldDetails != null ? List.copyOf(fieldDetails) : List.of();
        methodDetails = methodDetails != null ? List.copyOf(methodDetails) : List.of();
    }

    /**
     * Creates an aggregate component without field/method details (backward compatibility).
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @param references aggregate references
     * @param usesPorts ports used
     * @return the aggregate component
     */
    public static AggregateComponent of(
            String name, String packageName, int fields, List<String> references, List<String> usesPorts) {
        return new AggregateComponent(name, packageName, fields, references, usesPorts, List.of(), List.of());
    }

    /**
     * Creates an aggregate component with all details.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @param references aggregate references
     * @param usesPorts ports used
     * @param fieldDetails field details for rendering
     * @param methodDetails method details for rendering
     * @return the aggregate component
     */
    public static AggregateComponent of(
            String name,
            String packageName,
            int fields,
            List<String> references,
            List<String> usesPorts,
            List<FieldDetail> fieldDetails,
            List<MethodDetail> methodDetails) {
        return new AggregateComponent(name, packageName, fields, references, usesPorts, fieldDetails, methodDetails);
    }
}
