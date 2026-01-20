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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a method in a type.
 *
 * <p>This record captures the method's signature, including name, return type,
 * parameters, modifiers, annotations, and thrown exceptions.</p>
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
 *     List.of(TypeRef.of("java.lang.RuntimeException")));
 *
 * // Get method signature
 * String sig = findById.signature(); // "findById(Long)"
 * }</pre>
 *
 * @param name the method name
 * @param returnType the return type
 * @param parameters the method parameters (immutable)
 * @param modifiers the method modifiers (immutable)
 * @param annotations the annotations on this method (immutable)
 * @param documentation the method's documentation (if present)
 * @param thrownExceptions the exceptions declared to be thrown (immutable)
 * @since 4.1.0
 */
public record Method(
        String name,
        TypeRef returnType,
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        List<TypeRef> thrownExceptions) {

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
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        parameters = List.copyOf(parameters);
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        thrownExceptions = List.copyOf(thrownExceptions);
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
        return new Method(name, returnType, List.of(), Set.of(), List.of(), Optional.empty(), List.of());
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
}
