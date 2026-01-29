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

import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a method in a type.
 *
 * <p>This record captures the method's signature, including name, return type,
 * parameters, modifiers, annotations, thrown exceptions, and semantic roles.</p>
 *
 * <h2>Method Roles</h2>
 * <p>Methods can have semantic roles that describe their purpose:</p>
 * <ul>
 *   <li>{@link MethodRole#GETTER} - Property accessor (getX/isX)</li>
 *   <li>{@link MethodRole#SETTER} - Property mutator (setX)</li>
 *   <li>{@link MethodRole#FACTORY} - Static factory method</li>
 *   <li>{@link MethodRole#BUSINESS} - Domain business logic</li>
 *   <li>{@link MethodRole#VALIDATION} - Validation/invariant check</li>
 *   <li>{@link MethodRole#LIFECYCLE} - Lifecycle callback</li>
 *   <li>{@link MethodRole#OBJECT_METHOD} - equals/hashCode/toString</li>
 *   <li>{@link MethodRole#COMMAND} - CQRS command (modifies state)</li>
 *   <li>{@link MethodRole#QUERY} - CQRS query (reads state)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple method
 * Method getter = Method.of("getName", TypeRef.of("java.lang.String"));
 *
 * // Method with full details
 * Method findById = new Method(
 *     "findById",
 *     TypeRef.of("java.util.Optional"),
 *     List.of(Parameter.of("id", TypeRef.of("java.lang.Long"))),
 *     Set.of(Modifier.PUBLIC),
 *     List.of(Annotation.of("Override")),
 *     Optional.of("Finds an entity by ID"),
 *     List.of(TypeRef.of("java.lang.RuntimeException")),
 *     Set.of(MethodRole.QUERY));
 *
 * // Get method signature
 * String sig = findById.signature(); // "findById(Long)"
 *
 * // Check method roles
 * if (findById.hasRole(MethodRole.QUERY)) {
 *     // Handle query method
 * }
 * if (findById.isGetter() || findById.isSetter()) {
 *     // Skip accessors
 * }
 * }</pre>
 *
 * @param name the method name
 * @param returnType the return type
 * @param parameters the method parameters (immutable)
 * @param modifiers the method modifiers (immutable)
 * @param annotations the annotations on this method (immutable)
 * @param documentation the method's documentation (if present)
 * @param thrownExceptions the exceptions declared to be thrown (immutable)
 * @param roles the semantic roles of this method (immutable)
 * @param cyclomaticComplexity the cyclomatic complexity of the method body, if calculated
 * @param sourceLocation the source code location of this method (if available)
 * @since 4.1.0
 * @since 5.0.0 Added roles, cyclomaticComplexity, and sourceLocation parameters
 */
public record Method(
        String name,
        TypeRef returnType,
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        List<TypeRef> thrownExceptions,
        Set<MethodRole> roles,
        OptionalInt cyclomaticComplexity,
        Optional<SourceReference> sourceLocation) {

    /**
     * Creates a new Method.
     *
     * @param name the method name, must not be null or blank
     * @param returnType the return type, must not be null
     * @param parameters the parameters, must not be null
     * @param modifiers the modifiers, must not be null
     * @param annotations the annotations, must not be null
     * @param documentation the documentation, must not be null
     * @param thrownExceptions the thrown exceptions, must not be null
     * @param roles the semantic roles, must not be null
     * @param cyclomaticComplexity the cyclomatic complexity, must not be null
     * @param sourceLocation the source location, must not be null
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public Method {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(thrownExceptions, "thrownExceptions must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(cyclomaticComplexity, "cyclomaticComplexity must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        parameters = List.copyOf(parameters);
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        thrownExceptions = List.copyOf(thrownExceptions);
        roles = Set.copyOf(roles);
    }

    /**
     * Creates a method with the given name and return type, with no parameters or other attributes.
     *
     * @param name the method name
     * @param returnType the return type
     * @return a new Method
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     */
    public static Method of(String name, TypeRef returnType) {
        return new Method(
                name,
                returnType,
                List.of(),
                Set.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(),
                OptionalInt.empty(),
                Optional.empty());
    }

    /**
     * Creates a method with the given name, return type, and roles.
     *
     * @param name the method name
     * @param returnType the return type
     * @param roles the semantic roles
     * @return a new Method
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     * @since 5.0.0
     */
    public static Method of(String name, TypeRef returnType, Set<MethodRole> roles) {
        return new Method(
                name,
                returnType,
                List.of(),
                Set.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                roles,
                OptionalInt.empty(),
                Optional.empty());
    }

    /**
     * Creates a method with the given name, return type, roles, and cyclomatic complexity.
     *
     * @param name the method name
     * @param returnType the return type
     * @param roles the semantic roles
     * @param complexity the cyclomatic complexity
     * @return a new Method
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if name is blank
     * @since 5.0.0
     */
    public static Method of(String name, TypeRef returnType, Set<MethodRole> roles, int complexity) {
        return new Method(
                name,
                returnType,
                List.of(),
                Set.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                roles,
                OptionalInt.of(complexity),
                Optional.empty());
    }

    /**
     * Returns the method signature (name and parameter types).
     *
     * <p>The signature includes the method name followed by the simple names of
     * parameter types in parentheses. For example: {@code "findById(Long)"} or
     * {@code "transfer(Account, Account, BigDecimal)"}.</p>
     *
     * @return the method signature
     */
    public String signature() {
        String paramTypes = parameters.stream().map(p -> p.type().simpleName()).collect(Collectors.joining(", "));
        return name + "(" + paramTypes + ")";
    }

    /**
     * Returns whether this method is public.
     *
     * @return true if the method has the PUBLIC modifier
     */
    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Returns whether this method is abstract.
     *
     * @return true if the method has the ABSTRACT modifier
     */
    public boolean isAbstract() {
        return modifiers.contains(Modifier.ABSTRACT);
    }

    /**
     * Returns whether this method is static.
     *
     * @return true if the method has the STATIC modifier
     */
    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    /**
     * Returns whether this method has the given role.
     *
     * @param role the role to check
     * @return true if the method has this role
     * @since 5.0.0
     */
    public boolean hasRole(MethodRole role) {
        return roles.contains(role);
    }

    /**
     * Returns whether this method is a getter.
     *
     * @return true if the method has the GETTER role
     * @since 5.0.0
     */
    public boolean isGetter() {
        return hasRole(MethodRole.GETTER);
    }

    /**
     * Returns whether this method is a setter.
     *
     * @return true if the method has the SETTER role
     * @since 5.0.0
     */
    public boolean isSetter() {
        return hasRole(MethodRole.SETTER);
    }

    /**
     * Returns whether this method is a factory method.
     *
     * @return true if the method has the FACTORY role
     * @since 5.0.0
     */
    public boolean isFactory() {
        return hasRole(MethodRole.FACTORY);
    }

    /**
     * Returns whether this method is a business logic method.
     *
     * @return true if the method has the BUSINESS role
     * @since 5.0.0
     */
    public boolean isBusiness() {
        return hasRole(MethodRole.BUSINESS);
    }

    /**
     * Returns whether this method is an accessor (getter or query).
     *
     * @return true if the method has an accessor role
     * @since 5.0.0
     */
    public boolean isAccessor() {
        return roles.stream().anyMatch(MethodRole::isAccessor);
    }

    /**
     * Returns whether this method is a mutation (setter, command, or business).
     *
     * @return true if the method has a mutation role
     * @since 5.0.0
     */
    public boolean isMutation() {
        return roles.stream().anyMatch(MethodRole::isMutation);
    }

    /**
     * Returns whether this method is an Object method (equals, hashCode, toString).
     *
     * @return true if the method has the OBJECT_METHOD role
     * @since 5.0.0
     */
    public boolean isObjectMethod() {
        return hasRole(MethodRole.OBJECT_METHOD);
    }
}
