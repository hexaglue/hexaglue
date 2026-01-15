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
import java.util.Optional;
import java.util.Set;

/**
 * A method declared on a port interface with complete semantic information.
 *
 * <p>This record captures all information needed by plugins to generate adapter code,
 * including the method classification according to Spring Data conventions.
 *
 * @param name the method name (e.g., "findById", "existsByEmail")
 * @param returnType the return type with generics preserved
 * @param parameters the method parameters with names and type info
 * @param kind the method classification (FIND_BY_ID, EXISTS_BY_PROPERTY, etc.)
 * @param targetProperties properties targeted by property-based queries (e.g., ["email"] for findByEmail)
 * @param modifiers query modifiers (DISTINCT, IGNORE_CASE, etc.)
 * @param limitSize limit for Top/First queries (e.g., 10 for findTop10By...)
 * @param orderByProperty property for ordering (e.g., "age" for ...OrderByAge)
 * @param annotations key annotations on the method (@Transactional, etc.)
 * @since 3.0.0
 */
public record PortMethod(
        String name,
        TypeRef returnType,
        List<MethodParameter> parameters,
        MethodKind kind,
        List<String> targetProperties,
        Set<QueryModifier> modifiers,
        Optional<Integer> limitSize,
        Optional<String> orderByProperty,
        Set<String> annotations) {

    /**
     * Creates a minimal PortMethod for simple use cases.
     *
     * @param name the method name
     * @param returnType the return type
     * @param parameters the method parameters
     * @param kind the method classification
     * @return a new PortMethod with defaults for optional fields
     */
    public static PortMethod of(String name, TypeRef returnType, List<MethodParameter> parameters, MethodKind kind) {
        return new PortMethod(
                name, returnType, parameters, kind, List.of(), Set.of(), Optional.empty(), Optional.empty(), Set.of());
    }

    /**
     * Creates a PortMethod with a target property for property-based queries.
     *
     * @param name the method name
     * @param returnType the return type
     * @param parameters the method parameters
     * @param kind the method classification
     * @param targetProperty the target property name
     * @return a new PortMethod
     */
    public static PortMethod withProperty(
            String name, TypeRef returnType, List<MethodParameter> parameters, MethodKind kind, String targetProperty) {
        return new PortMethod(
                name,
                returnType,
                parameters,
                kind,
                List.of(targetProperty),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Set.of());
    }

    /**
     * Creates a PortMethod from legacy string-based representation.
     *
     * @param name the method name
     * @param returnType the qualified return type name
     * @param parameterTypes the qualified parameter type names
     * @return a new PortMethod with CUSTOM kind (classification to be done by Core)
     * @deprecated Use the full constructor or factory methods instead.
     */
    @Deprecated
    public static PortMethod legacy(String name, String returnType, List<String> parameterTypes) {
        TypeRef returnTypeRef = TypeRef.of(returnType);
        List<MethodParameter> params = parameterTypes.stream()
                .map(type -> MethodParameter.simple("arg", TypeRef.of(type)))
                .toList();
        return of(name, returnTypeRef, params, MethodKind.CUSTOM);
    }

    /**
     * Returns the first parameter, if any.
     *
     * @return the first parameter wrapped in Optional
     */
    public Optional<MethodParameter> firstParameter() {
        return parameters.isEmpty() ? Optional.empty() : Optional.of(parameters.get(0));
    }

    /**
     * Returns the first target property, if any.
     *
     * @return the first target property wrapped in Optional
     */
    public Optional<String> targetProperty() {
        return targetProperties.isEmpty() ? Optional.empty() : Optional.of(targetProperties.get(0));
    }

    /**
     * Returns true if the return type is Optional.
     *
     * @return true if returns Optional
     */
    public boolean returnsOptional() {
        return returnType.isOptionalLike();
    }

    /**
     * Returns true if the return type is a collection (List, Set, etc.).
     *
     * @return true if returns a collection
     */
    public boolean returnsCollection() {
        return returnType.isCollectionLike();
    }

    /**
     * Returns true if the return type is a Stream.
     *
     * @return true if returns a Stream
     */
    public boolean returnsStream() {
        return returnType.isStreamLike();
    }

    /**
     * Returns true if this is an ID-based method (findById, existsById, deleteById).
     *
     * @return true if the method operates on identifiers
     */
    public boolean isIdBased() {
        return kind == MethodKind.FIND_BY_ID
                || kind == MethodKind.EXISTS_BY_ID
                || kind == MethodKind.DELETE_BY_ID
                || kind == MethodKind.FIND_ALL_BY_ID;
    }

    /**
     * Returns true if this is a property-based method.
     *
     * @return true if the method operates on properties
     */
    public boolean isPropertyBased() {
        return kind == MethodKind.FIND_BY_PROPERTY
                || kind == MethodKind.FIND_ALL_BY_PROPERTY
                || kind == MethodKind.EXISTS_BY_PROPERTY
                || kind == MethodKind.COUNT_BY_PROPERTY
                || kind == MethodKind.DELETE_BY_PROPERTY
                || kind == MethodKind.STREAM_BY_PROPERTY;
    }
}
