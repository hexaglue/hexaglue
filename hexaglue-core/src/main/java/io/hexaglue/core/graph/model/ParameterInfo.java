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

package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * Information about a method or constructor parameter.
 *
 * @param name the parameter name
 * @param type the parameter type
 * @param annotations the annotations on the parameter
 */
public record ParameterInfo(String name, TypeRef type, List<AnnotationRef> annotations) {

    public ParameterInfo {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        annotations = annotations != null ? List.copyOf(annotations) : List.of();
    }

    /**
     * Creates a parameter with no annotations.
     */
    public static ParameterInfo of(String name, TypeRef type) {
        return new ParameterInfo(name, type, List.of());
    }

    /**
     * Returns the simple type name.
     */
    public String typeSimpleName() {
        return type.simpleName();
    }

    /**
     * Returns the qualified type name.
     */
    public String typeQualifiedName() {
        return type.rawQualifiedName();
    }
}
