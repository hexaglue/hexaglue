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

package io.hexaglue.spi.audit;

import java.util.List;

/**
 * Represents a code unit (class, interface, enum, record) being audited.
 *
 * <p>A code unit is the basic element of code analysis in the audit system.
 * It captures structural information, architectural classification, and
 * quality metrics.
 *
 * @param qualifiedName the fully qualified name
 * @param kind          the Java construct kind
 * @param layer         the architectural layer classification
 * @param role          the architectural role classification
 * @param methods       the method declarations
 * @param fields        the field declarations
 * @param metrics       the code quality metrics
 * @param documentation the documentation information
 * @since 3.0.0
 */
public record CodeUnit(
        String qualifiedName,
        CodeUnitKind kind,
        LayerClassification layer,
        RoleClassification role,
        List<MethodDeclaration> methods,
        List<FieldDeclaration> fields,
        CodeMetrics metrics,
        DocumentationInfo documentation) {

    /**
     * Compact constructor with defensive copies.
     */
    public CodeUnit {
        methods = methods != null ? List.copyOf(methods) : List.of();
        fields = fields != null ? List.copyOf(fields) : List.of();
    }

    /**
     * Returns the simple name of this code unit.
     *
     * @return the simple name
     */
    public String simpleName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Returns the package name of this code unit.
     *
     * @return the package name
     */
    public String packageName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
