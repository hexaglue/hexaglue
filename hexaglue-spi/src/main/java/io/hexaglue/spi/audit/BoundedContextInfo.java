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

package io.hexaglue.spi.audit;

import java.util.List;
import java.util.Objects;

/**
 * Represents a detected Bounded Context in the analyzed codebase.
 *
 * <p>A Bounded Context is a central pattern in Domain-Driven Design that defines
 * explicit boundaries within which a particular domain model is defined and applicable.
 * This record captures the essential information about a detected bounded context,
 * including its name, the package that defines its boundary, and the types it contains.
 *
 * <p><strong>Detection algorithm:</strong><br>
 * Bounded contexts are detected by analyzing the package structure:
 * <ul>
 *   <li>The third segment of a package name (e.g., "order" in "com.example.order.domain")
 *       identifies the bounded context</li>
 *   <li>All types within that package hierarchy belong to the context</li>
 * </ul>
 *
 * <p><strong>Example:</strong>
 * <pre>{@code
 * // Given types in these packages:
 * // - com.example.order.domain.Order
 * // - com.example.order.domain.OrderLine
 * // - com.example.inventory.domain.Product
 *
 * // Detected bounded contexts:
 * // - BoundedContextInfo("order", "com.example.order", ["Order", "OrderLine"])
 * // - BoundedContextInfo("inventory", "com.example.inventory", ["Product"])
 * }</pre>
 *
 * @param name           the name of the bounded context (typically derived from package structure)
 * @param rootPackage    the root package that defines this bounded context's boundary
 * @param typeNames      the fully-qualified names of all types in this bounded context
 * @since 3.0.0
 */
public record BoundedContextInfo(String name, String rootPackage, List<String> typeNames) {

    /**
     * Compact constructor with validation and defensive copy.
     *
     * @throws NullPointerException if name or rootPackage is null
     */
    public BoundedContextInfo {
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(rootPackage, "rootPackage required");
        typeNames = typeNames != null ? List.copyOf(typeNames) : List.of();
    }

    /**
     * Creates an empty bounded context with the given name and root package.
     *
     * @param name        the context name
     * @param rootPackage the root package
     * @return a new BoundedContextInfo with no types
     */
    public static BoundedContextInfo empty(String name, String rootPackage) {
        return new BoundedContextInfo(name, rootPackage, List.of());
    }

    /**
     * Returns the number of types in this bounded context.
     *
     * @return the count of types
     */
    public int typeCount() {
        return typeNames.size();
    }

    /**
     * Returns true if this bounded context contains no types.
     *
     * @return true if typeNames is empty
     */
    public boolean isEmpty() {
        return typeNames.isEmpty();
    }

    /**
     * Checks if a given type belongs to this bounded context.
     *
     * @param qualifiedName the fully-qualified type name
     * @return true if the type is in this bounded context
     */
    public boolean containsType(String qualifiedName) {
        return typeNames.contains(qualifiedName);
    }

    /**
     * Checks if a given package belongs to this bounded context.
     *
     * <p>A package belongs to this context if it equals the root package
     * or is a sub-package of it.
     *
     * @param packageName the package name to check
     * @return true if the package belongs to this context
     */
    public boolean containsPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.equals(rootPackage) || packageName.startsWith(rootPackage + ".");
    }
}
