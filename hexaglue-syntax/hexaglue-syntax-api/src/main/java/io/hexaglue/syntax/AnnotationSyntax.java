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

package io.hexaglue.syntax;

import java.util.Map;
import java.util.Optional;

/**
 * Syntactic representation of an annotation with ALL its values.
 *
 * <p>Unlike reflection which may lose annotation values at runtime,
 * this captures the full source representation.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AnnotationSyntax ann = ...;
 * String name = ann.qualifiedName(); // "javax.persistence.Entity"
 *
 * // Get String value
 * Optional<String> value = ann.getString("name");
 *
 * // Get enum value
 * Optional<FetchType> fetch = ann.getEnum("fetch", FetchType.class);
 * }</pre>
 *
 * @since 4.0.0
 */
public interface AnnotationSyntax {

    /**
     * Returns the fully qualified name of the annotation type.
     *
     * @return the qualified name (e.g., "javax.persistence.Entity")
     */
    String qualifiedName();

    /**
     * Returns the simple name of the annotation type.
     *
     * @return the simple name (e.g., "Entity")
     */
    String simpleName();

    /**
     * Returns all annotation values as a map.
     *
     * <p>The map includes both explicitly set values and default values
     * if available.</p>
     *
     * @return an immutable map of parameter names to values
     */
    Map<String, AnnotationValue> values();

    /**
     * Returns whether a specific parameter has a value.
     *
     * @param name the parameter name
     * @return true if the parameter has a value
     */
    default boolean hasValue(String name) {
        return values().containsKey(name);
    }

    /**
     * Returns the raw value of a parameter.
     *
     * @param name the parameter name
     * @return an Optional containing the value, or empty if not present
     */
    default Optional<AnnotationValue> getValue(String name) {
        return Optional.ofNullable(values().get(name));
    }

    // ===== Typed accessors =====

    /**
     * Returns a String parameter value.
     *
     * @param name the parameter name
     * @return an Optional containing the string, or empty if not present or not a string
     */
    default Optional<String> getString(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.StringValue)
                .map(v -> ((AnnotationValue.StringValue) v).value());
    }

    /**
     * Returns an integer parameter value.
     *
     * @param name the parameter name
     * @return an Optional containing the int, or empty if not present or not an int
     */
    default Optional<Integer> getInt(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.PrimitiveValue)
                .map(v -> ((AnnotationValue.PrimitiveValue) v).asInt());
    }

    /**
     * Returns a boolean parameter value.
     *
     * @param name the parameter name
     * @return an Optional containing the boolean, or empty if not present or not a boolean
     */
    default Optional<Boolean> getBoolean(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.PrimitiveValue)
                .map(v -> ((AnnotationValue.PrimitiveValue) v).asBoolean());
    }

    /**
     * Returns an enum parameter value.
     *
     * @param name the parameter name
     * @param enumType the enum class
     * @param <E> the enum type
     * @return an Optional containing the enum constant, or empty if not present or not an enum
     */
    default <E extends Enum<E>> Optional<E> getEnum(String name, Class<E> enumType) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.EnumValue)
                .map(v -> ((AnnotationValue.EnumValue) v).asEnum(enumType));
    }

    /**
     * Returns a class parameter value.
     *
     * @param name the parameter name
     * @return an Optional containing the type reference, or empty if not present or not a class
     */
    default Optional<TypeRef> getClass(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.ClassValue)
                .map(v -> ((AnnotationValue.ClassValue) v).typeRef());
    }

    /**
     * Returns a nested annotation parameter value.
     *
     * @param name the parameter name
     * @return an Optional containing the nested annotation, or empty if not present
     */
    default Optional<AnnotationSyntax> getNested(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.AnnotationRefValue)
                .map(v -> ((AnnotationValue.AnnotationRefValue) v).annotation());
    }

    /**
     * Returns a String array parameter value.
     *
     * @param name the parameter name
     * @return the list of strings (empty if not present or not a string array)
     */
    default java.util.List<String> getStringArray(String name) {
        return getValue(name)
                .filter(v -> v instanceof AnnotationValue.ArrayValue)
                .map(v -> ((AnnotationValue.ArrayValue) v).asStrings())
                .orElse(java.util.List.of());
    }
}
