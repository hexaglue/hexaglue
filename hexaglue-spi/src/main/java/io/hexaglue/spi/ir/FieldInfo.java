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

package io.hexaglue.spi.ir;

import java.util.Objects;
import java.util.Set;

/**
 * Information about a field in a domain type.
 *
 * <p>This record provides a simplified, stable representation of a field
 * for use in code generation and analysis. It captures the essential
 * characteristics without exposing implementation details.
 *
 * @param name        the field name
 * @param type        the field type reference
 * @param modifiers   the field modifiers (e.g., "private", "final", "static")
 * @param annotations the fully qualified annotation names on this field
 * @since 3.0.0
 */
public record FieldInfo(String name, TypeRef type, Set<String> modifiers, Set<String> annotations) {

    /**
     * Compact constructor with validation and defensive copies.
     *
     * @throws NullPointerException if any parameter is null
     */
    public FieldInfo {
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(type, "type required");
        modifiers = modifiers != null ? Set.copyOf(modifiers) : Set.of();
        annotations = annotations != null ? Set.copyOf(annotations) : Set.of();
    }

    /**
     * Creates a FieldInfo with no modifiers or annotations.
     *
     * @param name the field name
     * @param type the field type
     * @return new FieldInfo
     */
    public static FieldInfo simple(String name, TypeRef type) {
        return new FieldInfo(name, type, Set.of(), Set.of());
    }

    /**
     * Returns true if this field has the given modifier.
     *
     * @param modifier the modifier to check (e.g., "private", "final")
     * @return true if modifier is present
     */
    public boolean hasModifier(String modifier) {
        return modifiers.contains(modifier);
    }

    /**
     * Returns true if this field is final.
     *
     * @return true if "final" modifier is present
     */
    public boolean isFinal() {
        return hasModifier("final");
    }

    /**
     * Returns true if this field is static.
     *
     * @return true if "static" modifier is present
     */
    public boolean isStatic() {
        return hasModifier("static");
    }

    /**
     * Returns true if this field is private.
     *
     * @return true if "private" modifier is present
     */
    public boolean isPrivate() {
        return hasModifier("private");
    }

    /**
     * Returns true if this field is annotated with the given annotation.
     *
     * @param annotationQualifiedName the fully qualified annotation name
     * @return true if annotation is present
     */
    public boolean hasAnnotation(String annotationQualifiedName) {
        return annotations.contains(annotationQualifiedName);
    }

    /**
     * Returns true if this field has any annotations.
     *
     * @return true if annotations is not empty
     */
    public boolean isAnnotated() {
        return !annotations.isEmpty();
    }
}
