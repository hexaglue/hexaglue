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

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a use case exposed by a driving port.
 *
 * <p>A use case encapsulates a business operation that the application provides to
 * external actors. Use cases are typically derived from the methods declared on
 * driving ports (inbound interfaces).</p>
 *
 * <h2>Use Case Types (CQRS)</h2>
 * <ul>
 *   <li><strong>COMMAND</strong>: Operations that modify state</li>
 *   <li><strong>QUERY</strong>: Operations that read state without modification</li>
 *   <li><strong>COMMAND_QUERY</strong>: Operations that both modify and return state</li>
 * </ul>
 *
 * <h2>Detection Rules</h2>
 * <ul>
 *   <li>COMMAND: void return type with parameters</li>
 *   <li>QUERY: non-void return type, no state mutation</li>
 *   <li>COMMAND_QUERY: non-void return type, likely mutates state</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // From a driving port method
 * Method createOrder = port.structure().methods().get(0);
 * UseCase useCase = UseCase.of(createOrder, UseCaseType.COMMAND);
 *
 * // Derive type automatically
 * UseCaseType type = UseCase.deriveType(createOrder);
 * UseCase autoUseCase = UseCase.of(createOrder, type);
 *
 * // With description
 * UseCase described = new UseCase(
 *     createOrder,
 *     Optional.of("Creates a new order in the system"),
 *     UseCaseType.COMMAND
 * );
 * }</pre>
 *
 * @param method the method representing this use case
 * @param description an optional human-readable description
 * @param type the use case type (COMMAND, QUERY, or COMMAND_QUERY)
 * @since 5.0.0
 */
public record UseCase(Method method, Optional<String> description, UseCaseType type) {

    /**
     * The type of a use case based on CQRS principles.
     *
     * @since 5.0.0
     */
    public enum UseCaseType {
        /**
         * A command that modifies system state.
         *
         * <p>Commands typically have void return type and represent
         * state-changing operations like "create", "update", "delete".</p>
         */
        COMMAND,

        /**
         * A query that reads system state without modification.
         *
         * <p>Queries always return a value and should not have side effects.
         * Examples: "find", "get", "list", "search".</p>
         */
        QUERY,

        /**
         * An operation that both modifies and returns state.
         *
         * <p>This is a mixed operation that doesn't follow strict CQRS
         * separation. Examples: "createAndReturn", "updateAndGet".</p>
         */
        COMMAND_QUERY;

        /**
         * Returns whether this type represents a state-modifying operation.
         *
         * @return true if this is COMMAND or COMMAND_QUERY
         */
        public boolean isCommand() {
            return this == COMMAND || this == COMMAND_QUERY;
        }

        /**
         * Returns whether this type represents a state-reading operation.
         *
         * @return true if this is QUERY or COMMAND_QUERY
         */
        public boolean isQuery() {
            return this == QUERY || this == COMMAND_QUERY;
        }
    }

    /**
     * Creates a new UseCase.
     *
     * @param method the method, must not be null
     * @param description the description, must not be null (use Optional.empty() for none)
     * @param type the use case type, must not be null
     * @throws NullPointerException if any argument is null
     */
    public UseCase {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Creates a UseCase from a method with the given type.
     *
     * <p>The description is derived from the method's documentation if available.</p>
     *
     * @param method the method representing this use case
     * @param type the use case type
     * @return a new UseCase
     * @throws NullPointerException if any argument is null
     */
    public static UseCase of(Method method, UseCaseType type) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return new UseCase(method, method.documentation(), type);
    }

    /**
     * Creates a UseCase from a method, automatically deriving the type.
     *
     * @param method the method representing this use case
     * @return a new UseCase with derived type
     * @throws NullPointerException if method is null
     */
    public static UseCase from(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return of(method, deriveType(method));
    }

    /**
     * Derives the use case type from a method signature.
     *
     * <h2>Derivation Rules</h2>
     * <ol>
     *   <li>If return type is void → COMMAND</li>
     *   <li>If no parameters and has return → QUERY</li>
     *   <li>If method name starts with "get", "find", "list", "search", "count", "exists" → QUERY</li>
     *   <li>Otherwise → COMMAND_QUERY (conservative: has both parameters and return)</li>
     * </ol>
     *
     * @param method the method to analyze
     * @return the derived UseCaseType
     * @throws NullPointerException if method is null
     */
    public static UseCaseType deriveType(Method method) {
        Objects.requireNonNull(method, "method must not be null");

        boolean hasReturn = !isVoidReturn(method);
        boolean hasParameters = !method.parameters().isEmpty();
        String name = method.name().toLowerCase();

        // void return → COMMAND
        if (!hasReturn) {
            return UseCaseType.COMMAND;
        }

        // No parameters with return → QUERY
        if (!hasParameters) {
            return UseCaseType.QUERY;
        }

        // Query-like method names → QUERY
        if (isQueryMethodName(name)) {
            return UseCaseType.QUERY;
        }

        // Has both parameters and return → COMMAND_QUERY
        return UseCaseType.COMMAND_QUERY;
    }

    /**
     * Returns the use case name.
     *
     * <p>The name is derived from the method name.</p>
     *
     * @return the use case name
     */
    public String name() {
        return method.name();
    }

    /**
     * Returns whether this use case is a command.
     *
     * @return true if this is a COMMAND or COMMAND_QUERY
     */
    public boolean isCommand() {
        return type.isCommand();
    }

    /**
     * Returns whether this use case is a query.
     *
     * @return true if this is a QUERY or COMMAND_QUERY
     */
    public boolean isQuery() {
        return type.isQuery();
    }

    /**
     * Returns whether this use case has a description.
     *
     * @return true if a description is present
     */
    public boolean hasDescription() {
        return description.isPresent();
    }

    private static boolean isVoidReturn(Method method) {
        String returnType = method.returnType().qualifiedName();
        return "void".equals(returnType) || "java.lang.Void".equals(returnType);
    }

    private static boolean isQueryMethodName(String name) {
        return name.startsWith("get")
                || name.startsWith("find")
                || name.startsWith("list")
                || name.startsWith("search")
                || name.startsWith("count")
                || name.startsWith("exists")
                || name.startsWith("is")
                || name.startsWith("has")
                || name.startsWith("load")
                || name.startsWith("fetch")
                || name.startsWith("query")
                || name.startsWith("read");
    }
}
