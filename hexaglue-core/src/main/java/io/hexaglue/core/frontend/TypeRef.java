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

package io.hexaglue.core.frontend;

import java.util.List;
import java.util.Set;

/**
 * Reference to a type, including generic type arguments.
 *
 * @param rawQualifiedName the fully qualified name without type arguments (e.g., "java.util.List")
 * @param arguments type arguments if parameterized (e.g., [Order] for List&lt;Order&gt;)
 * @param isArray true if this is an array type
 * @param arrayDimensions number of array dimensions (0 if not an array)
 */
public record TypeRef(String rawQualifiedName, List<TypeRef> arguments, boolean isArray, int arrayDimensions) {

    private static final Set<String> OPTIONAL_TYPES =
            Set.of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble");

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "java.util.List",
            "java.util.Set",
            "java.util.Collection",
            "java.util.Iterable",
            "java.util.stream.Stream",
            "java.util.Queue",
            "java.util.Deque",
            "java.util.SortedSet",
            "java.util.NavigableSet");

    private static final Set<String> MAP_TYPES = Set.of(
            "java.util.Map", "java.util.SortedMap", "java.util.NavigableMap", "java.util.concurrent.ConcurrentMap");

    /**
     * Creates a simple (non-parameterized, non-array) type reference.
     */
    public static TypeRef of(String qualifiedName) {
        return new TypeRef(qualifiedName, List.of(), false, 0);
    }

    /**
     * Creates a parameterized type reference.
     */
    public static TypeRef parameterized(String rawQualifiedName, TypeRef... arguments) {
        return new TypeRef(rawQualifiedName, List.of(arguments), false, 0);
    }

    /**
     * Returns true if this type is parameterized (has type arguments).
     */
    public boolean isParameterized() {
        return !arguments.isEmpty();
    }

    /**
     * Returns true if this is an Optional-like wrapper type.
     */
    public boolean isOptionalLike() {
        return OPTIONAL_TYPES.contains(rawQualifiedName);
    }

    /**
     * Returns true if this is a Collection-like type.
     */
    public boolean isCollectionLike() {
        return COLLECTION_TYPES.contains(rawQualifiedName);
    }

    /**
     * Returns true if this is a Map-like type.
     */
    public boolean isMapLike() {
        return MAP_TYPES.contains(rawQualifiedName);
    }

    /**
     * Returns the simple name (without package).
     */
    public String simpleName() {
        int lastDot = rawQualifiedName.lastIndexOf('.');
        return lastDot < 0 ? rawQualifiedName : rawQualifiedName.substring(lastDot + 1);
    }

    /**
     * Returns the first type argument, or null if not parameterized.
     */
    public TypeRef firstArgument() {
        return arguments.isEmpty() ? null : arguments.get(0);
    }

    /**
     * Unwraps Optional/Collection to get the element type.
     */
    public TypeRef unwrapElement() {
        if ((isOptionalLike() || isCollectionLike()) && !arguments.isEmpty()) {
            return arguments.get(0);
        }
        return this;
    }
}
