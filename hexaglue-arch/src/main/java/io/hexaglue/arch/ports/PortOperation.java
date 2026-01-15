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

package io.hexaglue.arch.ports;

import io.hexaglue.syntax.MethodSyntax;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * An operation defined by a port interface.
 *
 * <p>Captures the contract of a single method in a port interface,
 * including its signature and parameter types.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Port interface:
 * public interface OrderRepository {
 *     Optional<Order> findById(OrderId id);
 *     void save(Order order);
 * }
 *
 * // Operations:
 * PortOperation findOp = new PortOperation(
 *     "findById",
 *     TypeRef.of("java.util.Optional"),
 *     List.of(TypeRef.of("com.example.OrderId")),
 *     methodSyntax
 * );
 * }</pre>
 *
 * @param name the operation name
 * @param returnType the return type (can be null for void)
 * @param parameterTypes the parameter types
 * @param syntax the method syntax information (nullable)
 * @since 4.0.0
 */
public record PortOperation(String name, TypeRef returnType, List<TypeRef> parameterTypes, MethodSyntax syntax) {

    /**
     * Creates a new PortOperation instance.
     *
     * @param name the operation name, must not be null or blank
     * @param returnType the return type (can be null for void)
     * @param parameterTypes the parameter types, must not be null
     * @param syntax the method syntax (can be null)
     * @throws NullPointerException if name or parameterTypes is null
     * @throws IllegalArgumentException if name is blank
     */
    public PortOperation {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(parameterTypes, "parameterTypes must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        parameterTypes = List.copyOf(parameterTypes);
    }

    /**
     * Returns whether this operation returns void.
     *
     * @return true if the return type is void
     */
    public boolean isVoid() {
        return returnType == null || "void".equals(returnType.qualifiedName());
    }

    /**
     * Creates a simple operation for testing.
     *
     * @param name the operation name
     * @return a new PortOperation
     */
    public static PortOperation of(String name) {
        return new PortOperation(name, null, List.of(), null);
    }

    /**
     * Creates an operation with return type.
     *
     * @param name the operation name
     * @param returnType the return type
     * @return a new PortOperation
     */
    public static PortOperation of(String name, TypeRef returnType) {
        return new PortOperation(name, returnType, List.of(), null);
    }
}
