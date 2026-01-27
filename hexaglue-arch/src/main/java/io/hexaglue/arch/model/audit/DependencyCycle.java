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

package io.hexaglue.arch.model.audit;

import java.util.List;

/**
 * Represents a detected dependency cycle.
 *
 * <p>A dependency cycle occurs when a chain of dependencies forms a loop.
 * Cycles are generally considered architectural smells and should be resolved
 * through dependency inversion or restructuring.
 *
 * <p>Example:
 * <pre>{@code
 * // Type-level cycle: OrderService → Order → OrderService
 * var cycle = new DependencyCycle(
 *     CycleKind.TYPE_LEVEL,
 *     List.of("OrderService", "Order", "OrderService")
 * );
 *
 * // Package cycle: com.app.orders → com.app.customers → com.app.orders
 * var packageCycle = new DependencyCycle(
 *     CycleKind.PACKAGE_LEVEL,
 *     List.of("com.app.orders", "com.app.customers", "com.app.orders")
 * );
 * }</pre>
 *
 * @param kind the kind of cycle
 * @param path the dependency path forming the cycle (first and last elements are the same)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record DependencyCycle(CycleKind kind, List<String> path) {

    /**
     * Returns the length of the cycle (excluding the repeated element).
     */
    public int length() {
        return path.size() - 1;
    }

    /**
     * Converts the cycle path to a human-readable string.
     *
     * <p>Format: "A → B → C → A"
     *
     * @return the cycle as a path string
     */
    public String toPathString() {
        return String.join(" → ", path);
    }

    /**
     * Returns true if this is a direct cycle (A → B → A).
     */
    public boolean isDirect() {
        return length() == 2;
    }

    /**
     * Returns the starting element of the cycle.
     */
    public String start() {
        return path.get(0);
    }
}
