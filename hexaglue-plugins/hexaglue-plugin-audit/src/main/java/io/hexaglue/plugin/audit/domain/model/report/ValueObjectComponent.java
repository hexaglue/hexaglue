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
 * Details about a value object component.
 *
 * @param name simple name of the value object
 * @param packageName fully qualified package name
 * @param fieldDetails detailed information about each field for diagram rendering
 * @since 5.0.0
 */
public record ValueObjectComponent(String name, String packageName, List<FieldDetail> fieldDetails) {

    /**
     * Creates a value object component with validation.
     */
    public ValueObjectComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        fieldDetails = fieldDetails != null ? List.copyOf(fieldDetails) : List.of();
    }

    /**
     * Creates a value object component without field details (backward compatibility).
     *
     * @param name simple name
     * @param packageName package name
     * @return the value object component
     */
    public static ValueObjectComponent of(String name, String packageName) {
        return new ValueObjectComponent(name, packageName, List.of());
    }

    /**
     * Creates a value object component with field details.
     *
     * @param name simple name
     * @param packageName package name
     * @param fieldDetails field details for rendering
     * @return the value object component
     */
    public static ValueObjectComponent of(String name, String packageName, List<FieldDetail> fieldDetails) {
        return new ValueObjectComponent(name, packageName, fieldDetails);
    }
}
