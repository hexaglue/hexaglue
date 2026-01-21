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
 * <h2>Typed Values (since 5.0.0)</h2>
 * <p>In addition to raw values, annotations now provide typed values through the
 * {@link AnnotationValue} sealed interface. This allows for type-safe access to
 * annotation attributes and proper handling of complex types like nested annotations,
 * arrays, and class references.</p>
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
 * // Accessing raw values (legacy)
 * if (table.hasValue("name")) {
 *     String tableName = (String) table.getValue("name").get();
 * }
 *
 * // Accessing typed values (since 5.0.0)
 * table.getTypedValue("name").ifPresent(value -> {
 *     if (value instanceof AnnotationValue.StringVal sv) {
 *         String tableName = sv.value();
 *     }
 * });
 * }</pre>
 *
 * @param simpleName the simple name (without package) of the annotation
 * @param qualifiedName the fully qualified name of the annotation
 * @param values the annotation attribute values as raw objects (immutable)
 * @param typedValues the annotation attribute values as typed {@link AnnotationValue} (immutable)
 * @since 4.1.0
 * @since 5.0.0 added typedValues parameter
 */
public record Annotation(
        String simpleName, String qualifiedName, Map<String, Object> values, Map<String, AnnotationValue> typedValues) {

    /**
     * Creates a new Annotation.
     *
     * @param simpleName the simple name
     * @param qualifiedName the fully qualified name, must not be null or blank
     * @param values the annotation values, must not be null
     * @param typedValues the typed annotation values, must not be null
     * @throws NullPointerException if qualifiedName, values, or typedValues is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public Annotation {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(values, "values must not be null");
        Objects.requireNonNull(typedValues, "typedValues must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
        values = Map.copyOf(values);
        typedValues = Map.copyOf(typedValues);
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
     * <p>Typed values are automatically derived from raw values using
     * {@link AnnotationValue#from(Object)}.</p>
     *
     * @param qualifiedName the fully qualified name of the annotation
     * @param values the annotation attribute values
     * @return a new Annotation
     * @throws NullPointerException if qualifiedName or values is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public static Annotation of(String qualifiedName, Map<String, Object> values) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(values, "values must not be null");
        String simpleName = extractSimpleName(qualifiedName);
        Map<String, AnnotationValue> typedValues = convertToTypedValues(values);
        return new Annotation(simpleName, qualifiedName, values, typedValues);
    }

    /**
     * Creates an annotation with the given qualified name, raw values, and typed values.
     *
     * @param qualifiedName the fully qualified name of the annotation
     * @param values the raw annotation attribute values
     * @param typedValues the typed annotation attribute values
     * @return a new Annotation
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if qualifiedName is blank
     * @since 5.0.0
     */
    public static Annotation of(
            String qualifiedName, Map<String, Object> values, Map<String, AnnotationValue> typedValues) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        String simpleName = extractSimpleName(qualifiedName);
        return new Annotation(simpleName, qualifiedName, values, typedValues);
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

    /**
     * Returns whether this annotation has a typed value for the given attribute name.
     *
     * @param name the attribute name to check
     * @return true if the attribute has a typed value
     * @since 5.0.0
     */
    public boolean hasTypedValue(String name) {
        return typedValues.containsKey(name);
    }

    /**
     * Returns the typed value for the given attribute name.
     *
     * @param name the attribute name
     * @return the typed value, or empty if not present
     * @since 5.0.0
     */
    public Optional<AnnotationValue> getTypedValue(String name) {
        return Optional.ofNullable(typedValues.get(name));
    }

    /**
     * Returns true if this annotation has any values (raw or typed).
     *
     * @return true if the annotation has values
     * @since 5.0.0
     */
    public boolean hasValues() {
        return !values.isEmpty() || !typedValues.isEmpty();
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private static Map<String, AnnotationValue> convertToTypedValues(Map<String, Object> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        return values.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, e -> AnnotationValue.from(e.getValue())));
    }
}
