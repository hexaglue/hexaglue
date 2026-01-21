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

/**
 * Represents the semantic role of a method in a domain type.
 *
 * <p>Method roles help classify methods by their purpose and behavior,
 * enabling code generation and analysis tools to treat different kinds
 * of methods appropriately.</p>
 *
 * <h2>Role Categories</h2>
 * <ul>
 *   <li><strong>Accessors</strong>: {@link #GETTER}, {@link #SETTER}, {@link #QUERY}</li>
 *   <li><strong>Mutation</strong>: {@link #SETTER}, {@link #COMMAND}, {@link #BUSINESS}</li>
 *   <li><strong>Creation</strong>: {@link #FACTORY}</li>
 *   <li><strong>Validation</strong>: {@link #VALIDATION}</li>
 *   <li><strong>Lifecycle</strong>: {@link #LIFECYCLE}</li>
 *   <li><strong>Infrastructure</strong>: {@link #OBJECT_METHOD}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Method method = ...;
 * if (method.hasRole(MethodRole.GETTER)) {
 *     // Generate getter accessor
 * }
 *
 * // Check for mutations
 * if (method.roles().stream().anyMatch(MethodRole::isMutation)) {
 *     // This method modifies state
 * }
 * }</pre>
 *
 * @since 5.0.0
 */
public enum MethodRole {

    /**
     * A getter method that returns the value of a property.
     *
     * <p>Detected by: name starting with "get" or "is" (for boolean),
     * no parameters, non-void return type.</p>
     */
    GETTER,

    /**
     * A setter method that modifies the value of a property.
     *
     * <p>Detected by: name starting with "set", exactly one parameter,
     * void return type.</p>
     */
    SETTER,

    /**
     * A factory method that creates instances of its declaring type.
     *
     * <p>Detected by: static method returning the declaring type or a subtype,
     * often named "of", "create", "from", "build", "newInstance".</p>
     */
    FACTORY,

    /**
     * A business logic method that performs domain operations.
     *
     * <p>This is the default role for methods that are not accessors,
     * factories, validation, or lifecycle methods. Business methods
     * typically implement domain rules and operations.</p>
     */
    BUSINESS,

    /**
     * A validation method that checks invariants or preconditions.
     *
     * <p>Detected by: name starting with "validate", "check", "ensure",
     * "verify", or "is" (when returning boolean and not a property getter).</p>
     */
    VALIDATION,

    /**
     * A lifecycle callback method.
     *
     * <p>Detected by: annotated with {@code @PostConstruct}, {@code @PreDestroy},
     * or named "init", "destroy", "close", "dispose".</p>
     */
    LIFECYCLE,

    /**
     * An Object method override (equals, hashCode, toString).
     *
     * <p>Detected by: methods named "equals", "hashCode", or "toString"
     * with matching signatures from {@link Object}.</p>
     */
    OBJECT_METHOD,

    /**
     * A command method that modifies state (CQRS pattern).
     *
     * <p>Detected by: void return type, parameters present, mutates state.
     * Commands typically represent state changes in the domain.</p>
     */
    COMMAND,

    /**
     * A query method that reads state without modification (CQRS pattern).
     *
     * <p>Detected by: non-void return type, typically annotated with
     * {@code @Transactional(readOnly = true)} or similar hints, or
     * methods on query interfaces/handlers.</p>
     */
    QUERY;

    /**
     * Returns whether this role represents a state-mutating operation.
     *
     * <p>Mutation roles are those that may change the state of an object
     * or the system: {@link #SETTER}, {@link #COMMAND}, and {@link #BUSINESS}.</p>
     *
     * @return {@code true} if this role represents a mutation operation
     */
    public boolean isMutation() {
        return this == SETTER || this == COMMAND || this == BUSINESS;
    }

    /**
     * Returns whether this role represents an accessor operation.
     *
     * <p>Accessor roles are those that read state without modification:
     * {@link #GETTER} and {@link #QUERY}.</p>
     *
     * @return {@code true} if this role represents an accessor operation
     */
    public boolean isAccessor() {
        return this == GETTER || this == QUERY;
    }

    /**
     * Returns whether this role represents an infrastructure method.
     *
     * <p>Infrastructure methods are not part of the domain logic but
     * are required for technical reasons: {@link #OBJECT_METHOD},
     * {@link #LIFECYCLE}, and {@link #FACTORY}.</p>
     *
     * @return {@code true} if this role represents an infrastructure method
     */
    public boolean isInfrastructure() {
        return this == OBJECT_METHOD || this == LIFECYCLE || this == FACTORY;
    }

    /**
     * Returns whether this role represents a domain operation.
     *
     * <p>Domain operations are methods that implement business logic:
     * {@link #BUSINESS}, {@link #COMMAND}, {@link #QUERY}, and {@link #VALIDATION}.</p>
     *
     * @return {@code true} if this role represents a domain operation
     */
    public boolean isDomainOperation() {
        return this == BUSINESS || this == COMMAND || this == QUERY || this == VALIDATION;
    }
}
