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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reference to an annotation on a node.
 *
 * @param qualifiedName the fully qualified name of the annotation type
 * @param simpleName the simple name of the annotation type
 * @param values the annotation attribute values (name → value)
 */
public record AnnotationRef(String qualifiedName, String simpleName, Map<String, Object> values) {

    public AnnotationRef {
        Objects.requireNonNull(qualifiedName, "qualifiedName cannot be null");
        Objects.requireNonNull(simpleName, "simpleName cannot be null");
        values = values != null ? Map.copyOf(values) : Map.of();
    }

    /**
     * Creates an annotation reference with no values.
     */
    public static AnnotationRef of(String qualifiedName) {
        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        return new AnnotationRef(qualifiedName, simpleName, Map.of());
    }

    /**
     * Creates an annotation reference with values.
     */
    public static AnnotationRef of(String qualifiedName, Map<String, Object> values) {
        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        return new AnnotationRef(qualifiedName, simpleName, values);
    }

    /**
     * Returns a string attribute value.
     */
    public Optional<String> getString(String name) {
        Object v = values.get(name);
        return v instanceof String s ? Optional.of(s) : Optional.empty();
    }

    /**
     * Returns a boolean attribute value.
     */
    public Optional<Boolean> getBoolean(String name) {
        Object v = values.get(name);
        return v instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the "value" attribute as a string.
     */
    public Optional<String> value() {
        return getString("value");
    }

    /**
     * Returns true if this annotation is from jMolecules DDD package.
     */
    public boolean isJMoleculesDdd() {
        return qualifiedName.startsWith("org.jmolecules.ddd.");
    }

    /**
     * Returns true if this annotation is from jMolecules architecture package.
     */
    public boolean isJMoleculesArchitecture() {
        return qualifiedName.startsWith("org.jmolecules.architecture.");
    }

    /**
     * Returns true if this is a jMolecules annotation.
     */
    public boolean isJMolecules() {
        return qualifiedName.startsWith("org.jmolecules.");
    }
}
