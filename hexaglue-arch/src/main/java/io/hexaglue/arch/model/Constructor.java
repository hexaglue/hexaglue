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
 * Representation of a constructor in a type.
 *
 * <p>This record captures the constructor's signature, including parameters,
 * modifiers, annotations, and thrown exceptions.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // No-arg constructor
 * Constructor noArg = Constructor.noArg();
 *
 * // Constructor with parameters
 * Constructor ctor = Constructor.of(List.of(
 *     Parameter.of("id", TypeRef.of("java.lang.Long")),
 *     Parameter.of("name", TypeRef.of("java.lang.String"))));
 *
 * // Get constructor signature
 * String sig = ctor.signature(); // "(Long, String)"
 * }</pre>
 *
 * @param parameters the constructor parameters (immutable)
 * @param modifiers the constructor modifiers (immutable)
 * @param annotations the annotations on this constructor (immutable)
 * @param documentation the constructor's documentation (if present)
 * @param thrownExceptions the exceptions declared to be thrown (immutable)
 * @param sourceLocation the source code location of this constructor (if available)
 * @since 4.1.0
 * @since 5.0.0 Added sourceLocation parameter
 */
public record Constructor(
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        List<TypeRef> thrownExceptions,
        Optional<SourceReference> sourceLocation) {

    /**
     * Creates a new Constructor.
     *
     * @param parameters the parameters, must not be null
     * @param modifiers the modifiers, must not be null
     * @param annotations the annotations, must not be null
     * @param documentation the documentation, must not be null
     * @param thrownExceptions the thrown exceptions, must not be null
     * @param sourceLocation the source location, must not be null
     * @throws NullPointerException if any argument is null
     */
    public Constructor {
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(thrownExceptions, "thrownExceptions must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        parameters = List.copyOf(parameters);
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        thrownExceptions = List.copyOf(thrownExceptions);
    }

    /**
     * Creates a constructor with the given parameters and no other attributes.
     *
     * @param parameters the constructor parameters
     * @return a new Constructor
     * @throws NullPointerException if parameters is null
     */
    public static Constructor of(List<Parameter> parameters) {
        return new Constructor(parameters, Set.of(), List.of(), Optional.empty(), List.of(), Optional.empty());
    }

    /**
     * Creates a no-argument constructor.
     *
     * @return a new no-arg Constructor
     */
    public static Constructor noArg() {
        return of(List.of());
    }

    /**
     * Returns the constructor signature (parameter types).
     *
     * <p>The signature includes the simple names of parameter types in parentheses.
     * For example: {@code "(Long, String)"} or {@code "()"}.</p>
     *
     * @return the constructor signature
     */
    public String signature() {
        String paramTypes = parameters.stream().map(p -> p.type().simpleName()).collect(Collectors.joining(", "));
        return "(" + paramTypes + ")";
    }
}
