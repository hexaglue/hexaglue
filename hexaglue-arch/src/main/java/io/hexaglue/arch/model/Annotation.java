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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Representation of an annotation on a type, field, method, or parameter.
 *
 * <p>This record captures both the annotation's qualified name and its attribute values,
 * allowing code generation to preserve or transform annotations appropriately.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple annotation without values
 * Annotation entity = Annotation.of("javax.persistence.Entity");
 *
 * // Annotation with values
 * Annotation table = Annotation.of("javax.persistence.Table",
 *     Map.of("name", "orders", "schema", "public"));
 *
 * // Accessing values
 * if (table.hasValue("name")) {
 *     String tableName = (String) table.getValue("name").get();
 * }
 * }</pre>
 *
 * @param simpleName the simple name (without package) of the annotation
 * @param qualifiedName the fully qualified name of the annotation
 * @param values the annotation attribute values (immutable)
 * @since 4.1.0
 */
public record Annotation(String simpleName, String qualifiedName, Map<String, Object> values) {

    /**
     * Creates a new Annotation.
     *
     * @param simpleName the simple name
     * @param qualifiedName the fully qualified name, must not be null or blank
     * @param values the annotation values, must not be null
     * @throws NullPointerException if qualifiedName or values is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public Annotation {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(values, "values must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
        values = Map.copyOf(values);
    }

    /**
     * Creates an annotation with the given qualified name and no values.
     *
     * @param qualifiedName the fully qualified name of the annotation
     * @return a new Annotation
     * @throws NullPointerException if qualifiedName is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public static Annotation of(String qualifiedName) {
        return of(qualifiedName, Map.of());
    }

    /**
     * Creates an annotation with the given qualified name and values.
     *
     * @param qualifiedName the fully qualified name of the annotation
     * @param values the annotation attribute values
     * @return a new Annotation
     * @throws NullPointerException if qualifiedName or values is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public static Annotation of(String qualifiedName, Map<String, Object> values) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        String simpleName = extractSimpleName(qualifiedName);
        return new Annotation(simpleName, qualifiedName, values);
    }

    /**
     * Returns whether this annotation has a value for the given attribute name.
     *
     * @param name the attribute name to check
     * @return true if the attribute has a value
     */
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    /**
     * Returns the value for the given attribute name.
     *
     * @param name the attribute name
     * @return the value, or empty if not present
     */
    public Optional<Object> getValue(String name) {
        return Optional.ofNullable(values.get(name));
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
