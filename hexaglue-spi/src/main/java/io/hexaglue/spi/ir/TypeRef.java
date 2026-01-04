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
import java.util.Set;

/**
 * Reference to a type with support for generics and type analysis.
 *
 * <p>This is the SPI version of TypeRef, providing type information to plugins
 * without exposing internal implementation details.
 *
 * @param qualifiedName the fully qualified type name (e.g., "java.util.List")
 * @param simpleName the simple name without package (e.g., "List")
 * @param typeArguments type arguments if parameterized (e.g., [Order] for List&lt;Order&gt;)
 * @param primitive true if this is a primitive type
 * @param array true if this is an array type
 * @param arrayDimensions number of array dimensions (0 if not array)
 * @param cardinality the cardinality (SINGLE, OPTIONAL, COLLECTION)
 */
public record TypeRef(
        String qualifiedName,
        String simpleName,
        List<TypeRef> typeArguments,
        boolean primitive,
        boolean array,
        int arrayDimensions,
        Cardinality cardinality) {

    private static final Set<String> OPTIONAL_TYPES =
            Set.of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble");

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "java.util.List",
            "java.util.Set",
            "java.util.Collection",
            "java.util.Iterable",
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.HashSet",
            "java.util.TreeSet",
            "java.util.Queue",
            "java.util.Deque");

    private static final Set<String> MAP_TYPES =
            Set.of("java.util.Map", "java.util.HashMap", "java.util.TreeMap", "java.util.LinkedHashMap");

    /**
     * Creates a simple (non-parameterized) type reference.
     */
    public static TypeRef of(String qualifiedName) {
        String simpleName = extractSimpleName(qualifiedName);
        boolean primitive = isPrimitiveType(qualifiedName);
        return new TypeRef(qualifiedName, simpleName, List.of(), primitive, false, 0, Cardinality.SINGLE);
    }

    /**
     * Creates a type reference for a primitive type.
     */
    public static TypeRef primitive(String name) {
        return new TypeRef(name, name, List.of(), true, false, 0, Cardinality.SINGLE);
    }

    /**
     * Creates a parameterized type reference (e.g., List&lt;Order&gt;).
     */
    public static TypeRef parameterized(String qualifiedName, TypeRef... arguments) {
        String simpleName = extractSimpleName(qualifiedName);
        Cardinality cardinality = inferCardinality(qualifiedName);
        return new TypeRef(qualifiedName, simpleName, List.of(arguments), false, false, 0, cardinality);
    }

    /**
     * Creates a parameterized type reference with cardinality.
     */
    public static TypeRef parameterized(String qualifiedName, Cardinality cardinality, List<TypeRef> arguments) {
        String simpleName = extractSimpleName(qualifiedName);
        return new TypeRef(qualifiedName, simpleName, arguments, false, false, 0, cardinality);
    }

    /**
     * Creates an array type reference.
     */
    public static TypeRef array(TypeRef componentType, int dimensions) {
        return new TypeRef(
                componentType.qualifiedName(),
                componentType.simpleName(),
                List.of(),
                componentType.primitive(),
                true,
                dimensions,
                Cardinality.COLLECTION);
    }

    /**
     * Returns true if this type is parameterized (has type arguments).
     */
    public boolean isParameterized() {
        return !typeArguments.isEmpty();
    }

    /**
     * Returns true if this is an Optional-like wrapper type.
     */
    public boolean isOptionalLike() {
        return OPTIONAL_TYPES.contains(qualifiedName);
    }

    /**
     * Returns true if this is a Collection-like type.
     */
    public boolean isCollectionLike() {
        return COLLECTION_TYPES.contains(qualifiedName) || array;
    }

    /**
     * Returns true if this is a Map-like type.
     */
    public boolean isMapLike() {
        return MAP_TYPES.contains(qualifiedName);
    }

    /**
     * Returns the first type argument, or null if not parameterized.
     */
    public TypeRef firstArgument() {
        return typeArguments.isEmpty() ? null : typeArguments.get(0);
    }

    /**
     * Unwraps Optional/Collection to get the element type.
     * Returns this if not a wrapper type.
     */
    public TypeRef unwrapElement() {
        if ((isOptionalLike() || isCollectionLike()) && !typeArguments.isEmpty()) {
            return typeArguments.get(0);
        }
        return this;
    }

    /**
     * Returns true if this type is the same as the given qualified name.
     */
    public boolean is(String qualifiedName) {
        return this.qualifiedName.equals(qualifiedName);
    }

    /**
     * Returns true if this type is a subtype/same as a collection containing the given element type.
     */
    public boolean isCollectionOf(String elementQualifiedName) {
        if (!isCollectionLike()) {
            return false;
        }
        TypeRef element = unwrapElement();
        return element != this && element.qualifiedName().equals(elementQualifiedName);
    }

    /**
     * Returns true if this type requires an import (not a primitive, not in java.lang).
     */
    public boolean requiresImport() {
        if (primitive) {
            return false;
        }
        return !qualifiedName.startsWith("java.lang.") || qualifiedName.indexOf('.', 10) > 0;
    }

    /**
     * Returns the package name, or empty string for primitives.
     */
    public String packageName() {
        if (primitive) {
            return "";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? "" : qualifiedName.substring(0, lastDot);
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }

    private static boolean isPrimitiveType(String name) {
        return switch (name) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char", "void" -> true;
            default -> false;
        };
    }

    private static Cardinality inferCardinality(String qualifiedName) {
        if (OPTIONAL_TYPES.contains(qualifiedName)) {
            return Cardinality.OPTIONAL;
        }
        if (COLLECTION_TYPES.contains(qualifiedName)) {
            return Cardinality.COLLECTION;
        }
        return Cardinality.SINGLE;
    }
}
