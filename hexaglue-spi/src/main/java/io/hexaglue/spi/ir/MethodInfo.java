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

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Information about a method in a domain type.
 *
 * <p>This record provides a simplified, stable representation of a method
 * for use in code generation and analysis. It captures the method signature
 * and metadata without exposing implementation details.
 *
 * @param name           the method name
 * @param returnType     the return type reference
 * @param parameterTypes the parameter type references (in order)
 * @param modifiers      the method modifiers (e.g., "public", "abstract", "static")
 * @param annotations    the fully qualified annotation names on this method
 * @since 3.0.0
 */
public record MethodInfo(
        String name, TypeRef returnType, List<TypeRef> parameterTypes, Set<String> modifiers, Set<String> annotations) {

    /**
     * Compact constructor with validation and defensive copies.
     *
     * @throws NullPointerException if any parameter is null
     */
    public MethodInfo {
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(returnType, "returnType required");
        parameterTypes = parameterTypes != null ? List.copyOf(parameterTypes) : List.of();
        modifiers = modifiers != null ? Set.copyOf(modifiers) : Set.of();
        annotations = annotations != null ? Set.copyOf(annotations) : Set.of();
    }

    /**
     * Creates a MethodInfo with no parameters, modifiers, or annotations.
     *
     * @param name       the method name
     * @param returnType the return type
     * @return new MethodInfo
     */
    public static MethodInfo simple(String name, TypeRef returnType) {
        return new MethodInfo(name, returnType, List.of(), Set.of(), Set.of());
    }

    /**
     * Returns true if this method has the given modifier.
     *
     * @param modifier the modifier to check (e.g., "public", "abstract")
     * @return true if modifier is present
     */
    public boolean hasModifier(String modifier) {
        return modifiers.contains(modifier);
    }

    /**
     * Returns true if this method is abstract.
     *
     * @return true if "abstract" modifier is present
     */
    public boolean isAbstract() {
        return hasModifier("abstract");
    }

    /**
     * Returns true if this method is static.
     *
     * @return true if "static" modifier is present
     */
    public boolean isStatic() {
        return hasModifier("static");
    }

    /**
     * Returns true if this method is public.
     *
     * @return true if "public" modifier is present
     */
    public boolean isPublic() {
        return hasModifier("public");
    }

    /**
     * Returns true if this method is private.
     *
     * @return true if "private" modifier is present
     */
    public boolean isPrivate() {
        return hasModifier("private");
    }

    /**
     * Returns true if this method is annotated with the given annotation.
     *
     * @param annotationQualifiedName the fully qualified annotation name
     * @return true if annotation is present
     */
    public boolean hasAnnotation(String annotationQualifiedName) {
        return annotations.contains(annotationQualifiedName);
    }

    /**
     * Returns true if this method has any annotations.
     *
     * @return true if annotations is not empty
     */
    public boolean isAnnotated() {
        return !annotations.isEmpty();
    }

    /**
     * Returns the number of parameters.
     *
     * @return the parameter count
     */
    public int parameterCount() {
        return parameterTypes.size();
    }

    /**
     * Returns true if this method has no parameters.
     *
     * @return true if parameter count is 0
     */
    public boolean hasNoParameters() {
        return parameterTypes.isEmpty();
    }

    /**
     * Returns a method signature string for display purposes.
     *
     * <p>Format: {@code methodName(Type1, Type2, ...): ReturnType}
     *
     * @return the method signature
     */
    public String signature() {
        String params = parameterTypes.stream()
                .map(TypeRef::simpleName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return "%s(%s): %s".formatted(name, params, returnType.simpleName());
    }
}
