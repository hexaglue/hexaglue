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

package io.hexaglue.plugin.jpa.util;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.ir.Identity;
import io.hexaglue.arch.model.ir.TypeRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversion utilities for mapping domain types to JPA types using JavaPoet.
 *
 * <p>This utility class provides centralized type mapping logic to ensure consistency
 * across all JPA code generation. It leverages the complete TypeRef API from SPI 2.0.0
 * to handle collections, optionals, maps, and primitive types correctly.
 *
 * <h3>Key Capabilities:</h3>
 * <ul>
 *   <li>Domain to JPA type conversion with common type mappings</li>
 *   <li>Collection type handling (List, Set, Collection)</li>
 *   <li>Optional type unwrapping (JPA doesn't support Optional fields)</li>
 *   <li>Map type conversion</li>
 *   <li>Identity wrapper unwrapping (OrderId → UUID)</li>
 *   <li>Import requirement detection</li>
 * </ul>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Uses TypeRef SPI helpers ({@code unwrapElement()}, {@code isOptionalLike()}) instead of manual parsing</li>
 *   <li>Collections are always mapped to {@code List} for JPA compatibility</li>
 *   <li>Optionals are unwrapped because JPA does not support Optional field types</li>
 *   <li>Primitive types are preserved when appropriate for performance</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class TypeMappings {

    /**
     * Common domain-to-JPA type mappings.
     *
     * <p>This map contains the most frequently used Java types in domain models
     * and their corresponding JavaPoet TypeName representations.
     */
    private static final Map<String, TypeName> DOMAIN_TO_JPA = Map.ofEntries(
            Map.entry("java.util.UUID", TypeName.get(UUID.class)),
            Map.entry("java.lang.String", TypeName.get(String.class)),
            Map.entry("java.lang.Long", TypeName.get(Long.class)),
            Map.entry("java.lang.Integer", TypeName.get(Integer.class)),
            Map.entry("java.lang.Boolean", TypeName.get(Boolean.class)),
            Map.entry("java.lang.Double", TypeName.get(Double.class)),
            Map.entry("java.lang.Float", TypeName.get(Float.class)),
            Map.entry("java.time.Instant", TypeName.get(Instant.class)),
            Map.entry("java.time.LocalDate", TypeName.get(LocalDate.class)),
            Map.entry("java.time.LocalDateTime", TypeName.get(LocalDateTime.class)),
            Map.entry("java.math.BigDecimal", TypeName.get(BigDecimal.class)));

    private TypeMappings() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a qualified type name to a JavaPoet TypeName.
     *
     * <p>This method handles simple type resolution without considering generics.
     * For parameterized types, use {@link #toJpaType(TypeRef)} instead.
     *
     * <p>Handles primitives, common Java types, and custom types:
     * <ul>
     *   <li>Primitives: {@code int}, {@code boolean}, etc. → {@code TypeName.INT}, {@code TypeName.BOOLEAN}</li>
     *   <li>Common types: {@code java.util.UUID} → {@code TypeName.get(UUID.class)}</li>
     *   <li>Custom types: {@code com.example.Order} → {@code ClassName.bestGuess("com.example.Order")}</li>
     * </ul>
     *
     * @param qualifiedName the fully qualified type name
     * @return the corresponding JavaPoet TypeName
     * @throws IllegalArgumentException if qualifiedName is null or empty
     */
    public static TypeName toJpaType(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new IllegalArgumentException("Qualified name cannot be null or empty");
        }

        // Try primitives first
        try {
            return toPrimitiveTypeName(qualifiedName);
        } catch (IllegalArgumentException e) {
            // Not a primitive, continue to common types
        }

        // Try common types, fallback to ClassName.bestGuess
        return DOMAIN_TO_JPA.getOrDefault(qualifiedName, ClassName.bestGuess(qualifiedName));
    }

    /**
     * Converts a TypeRef from the SPI to a JavaPoet TypeName with full generic support.
     *
     * <p>This method is the primary type conversion entry point. It handles:
     * <ul>
     *   <li><b>Collections:</b> {@code List<Order>}, {@code Set<Tag>} → {@code List<Order>}, {@code Set<Tag>}</li>
     *   <li><b>Optionals:</b> {@code Optional<Order>} → {@code Order} (unwrapped for JPA)</li>
     *   <li><b>Maps:</b> {@code Map<String, Order>} → {@code Map<String, Order>}</li>
     *   <li><b>Primitives:</b> {@code int}, {@code boolean} → {@code TypeName.INT}, {@code TypeName.BOOLEAN}</li>
     *   <li><b>Simple types:</b> {@code UUID}, {@code String} → {@code TypeName.get(UUID.class)}</li>
     * </ul>
     *
     * <p>Uses TypeRef SPI helpers to avoid manual type argument parsing:
     * <ul>
     *   <li>{@link TypeRef#unwrapElement()} to extract collection/optional element types</li>
     *   <li>{@link TypeRef#isCollectionLike()} to detect collections</li>
     *   <li>{@link TypeRef#isOptionalLike()} to detect optionals</li>
     *   <li>{@link TypeRef#isMapLike()} to detect maps</li>
     * </ul>
     *
     * @param typeRef the type reference from the SPI
     * @return the corresponding JavaPoet TypeName (possibly parameterized)
     * @throws IllegalArgumentException if typeRef is null
     */
    public static TypeName toJpaType(TypeRef typeRef) {
        if (typeRef == null) {
            throw new IllegalArgumentException("TypeRef cannot be null");
        }

        // Handle primitives first (no generics)
        if (typeRef.primitive()) {
            return toPrimitiveTypeName(typeRef.qualifiedName());
        }

        // Handle collections (List, Set, Collection)
        if (typeRef.isCollectionLike()) {
            TypeRef element = typeRef.unwrapElement(); // Use SPI helper instead of arguments().get(0)

            // Recursively unwrap if element is Optional (e.g., List<Optional<Order>> → List<Order>)
            TypeRef unwrappedElement = element;
            while (unwrappedElement.isOptionalLike()) {
                unwrappedElement = unwrappedElement.unwrapElement();
            }

            TypeName elementType = toJpaType(unwrappedElement);

            // Determine the collection interface type
            String collectionType = typeRef.qualifiedName();
            if (collectionType.contains("Set")) {
                return ParameterizedTypeName.get(ClassName.get(java.util.Set.class), elementType);
            }
            // Default to List for List, Collection, Iterable
            return ParameterizedTypeName.get(ClassName.get(List.class), elementType);
        }

        // Handle optionals (unwrap for JPA - JPA doesn't support Optional fields)
        if (typeRef.isOptionalLike()) {
            TypeRef element = typeRef.unwrapElement();

            // If the unwrapped element is a collection, recursively process it
            // (e.g., Optional<List<Order>> → List<Order>)
            return toJpaType(element); // Return unwrapped type
        }

        // Handle maps
        if (typeRef.isMapLike()) {
            TypeRef keyType = typeRef.firstArgument();
            TypeRef valueType = typeRef.typeArguments().get(1);
            return ParameterizedTypeName.get(ClassName.get(Map.class), toJpaType(keyType), toJpaType(valueType));
        }

        // Handle simple types
        return toJpaType(typeRef.qualifiedName());
    }

    /**
     * Unwraps an identifier to its underlying JPA-compatible type.
     *
     * <p>Domain identifiers are often wrapped in custom types for type safety:
     * <pre>{@code
     * record OrderId(UUID value) {}  // Wrapped
     * private UUID id;                // Unwrapped
     * }</pre>
     *
     * <p>JPA requires the unwrapped type for {@code @Id} fields. This method
     * uses the SPI's {@link Identity#isWrapped()} helper to determine whether
     * unwrapping is needed.
     *
     * <p>Examples:
     * <ul>
     *   <li>Wrapped {@code OrderId(UUID value)} → {@code TypeName.get(UUID.class)}</li>
     *   <li>Unwrapped {@code UUID id} → {@code TypeName.get(UUID.class)}</li>
     *   <li>Wrapped {@code TaskId(Long value)} → {@code TypeName.get(Long.class)}</li>
     * </ul>
     *
     * @param identity the identity metadata from the SPI
     * @return the unwrapped JavaPoet TypeName suitable for JPA
     * @throws IllegalArgumentException if identity is null
     */
    public static TypeName unwrapIdentifier(Identity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity cannot be null");
        }

        // Use SPI helper to check if wrapped
        TypeRef effectiveType = identity.isWrapped() ? identity.unwrappedType() : identity.type();
        return toJpaType(effectiveType);
    }

    /**
     * Determines if a TypeRef requires an import statement.
     *
     * <p>Delegates to the SPI's {@link TypeRef#requiresImport()} method, which handles:
     * <ul>
     *   <li>Primitives (int, boolean, etc.) → no import needed</li>
     *   <li>java.lang types (String, Integer, etc.) → no import needed</li>
     *   <li>Other types → import needed</li>
     * </ul>
     *
     * <p>Design decision: Rely on SPI logic rather than duplicating import rules.
     *
     * @param typeRef the type reference to check
     * @return true if an import statement is required
     * @throws IllegalArgumentException if typeRef is null
     */
    public static boolean requiresImport(TypeRef typeRef) {
        if (typeRef == null) {
            throw new IllegalArgumentException("TypeRef cannot be null");
        }

        return typeRef.requiresImport();
    }

    /**
     * Extracts the package name from a TypeRef.
     *
     * <p>Delegates to the SPI's {@link TypeRef#packageName()} method.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code java.util.UUID} → {@code "java.util"}</li>
     *   <li>{@code com.example.Order} → {@code "com.example"}</li>
     *   <li>{@code int} → {@code ""} (primitives have no package)</li>
     * </ul>
     *
     * @param typeRef the type reference
     * @return the package name, or empty string for primitives
     * @throws IllegalArgumentException if typeRef is null
     */
    public static String packageName(TypeRef typeRef) {
        if (typeRef == null) {
            throw new IllegalArgumentException("TypeRef cannot be null");
        }

        return typeRef.packageName();
    }

    /**
     * Extracts the element type from a collection or optional TypeRef.
     *
     * <p>This is a convenience wrapper around {@link TypeRef#unwrapElement()}
     * that returns a JavaPoet TypeName instead of a TypeRef.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code List<Order>} → {@code TypeName for Order}</li>
     *   <li>{@code Set<Tag>} → {@code TypeName for Tag}</li>
     *   <li>{@code Optional<Customer>} → {@code TypeName for Customer}</li>
     *   <li>{@code String} → {@code TypeName for String} (unchanged)</li>
     * </ul>
     *
     * @param typeRef the collection or optional type reference
     * @return the unwrapped element type as JavaPoet TypeName
     * @throws IllegalArgumentException if typeRef is null
     */
    public static TypeName unwrapElement(TypeRef typeRef) {
        if (typeRef == null) {
            throw new IllegalArgumentException("TypeRef cannot be null");
        }

        return toJpaType(typeRef.unwrapElement());
    }

    /**
     * Converts a primitive type name to JavaPoet TypeName.
     *
     * <p>Handles all Java primitive types:
     * int, long, short, byte, boolean, char, float, double, void.
     *
     * @param primitiveName the primitive type name
     * @return the corresponding JavaPoet primitive TypeName
     * @throws IllegalArgumentException if the name is not a recognized primitive
     */
    private static TypeName toPrimitiveTypeName(String primitiveName) {
        return switch (primitiveName) {
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "short" -> TypeName.SHORT;
            case "byte" -> TypeName.BYTE;
            case "boolean" -> TypeName.BOOLEAN;
            case "char" -> TypeName.CHAR;
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            case "void" -> TypeName.VOID;
            default -> throw new IllegalArgumentException("Unknown primitive type: " + primitiveName);
        };
    }
}
