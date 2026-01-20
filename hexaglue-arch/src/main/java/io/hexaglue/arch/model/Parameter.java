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

package io.hexaglue.arch.model;

import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a method or constructor parameter.
 *
 * <p>This record captures the parameter's name, type, and any annotations
 * that may affect code generation.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple parameter
 * Parameter orderId = Parameter.of("orderId", TypeRef.of("com.example.OrderId"));
 *
 * // Parameter with annotations
 * Parameter param = new Parameter("value",
 *     TypeRef.of("java.lang.String"),
 *     List.of(Annotation.of("javax.annotation.Nonnull")));
 *
 * // Check for annotation
 * if (param.hasAnnotation("javax.annotation.Nonnull")) {
 *     // Generate null check
 * }
 * }</pre>
 *
 * @param name the parameter name
 * @param type the parameter type
 * @param annotations the annotations on this parameter (immutable)
 * @since 4.1.0
 */
public record Parameter(String name, TypeRef type, List<Annotation> annotations) {

    /**
     * Creates a new Parameter.
     *
     * @param name the parameter name, must not be null or blank
     * @param type the parameter type, must not be null
     * @param annotations the parameter annotations, must not be null
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public Parameter {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        annotations = List.copyOf(annotations);
    }

    /**
     * Creates a parameter with the given name and type, without annotations.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @return a new Parameter
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public static Parameter of(String name, TypeRef type) {
        return new Parameter(name, type, List.of());
    }

    /**
     * Returns whether this parameter has an annotation with the given qualified name.
     *
     * @param qualifiedName the fully qualified annotation name
     * @return true if the parameter has the annotation
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(a -> a.qualifiedName().equals(qualifiedName));
    }
}
