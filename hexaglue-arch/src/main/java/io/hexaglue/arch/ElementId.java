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

package io.hexaglue.arch;

import java.util.Objects;

/**
 * Stable identifier for any architectural element.
 *
 * <h2>Design Rationale (v2.1)</h2>
 * <p>The identity of an element does NOT depend on its classification. If heuristics evolve
 * and {@code Order} is reclassified from ENTITY to AGGREGATE_ROOT, the ElementId remains
 * stable: {@code "com.example.Order"}.</p>
 *
 * <p>This guarantees:</p>
 * <ul>
 *   <li>Stable references even if classification evolves</li>
 *   <li>Coherent caches and diffs between versions</li>
 *   <li>Robust serialization/deserialization</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ElementId id = ElementId.of("com.example.Order");
 * assert id.simpleName().equals("Order");
 * assert id.packageName().equals("com.example");
 * }</pre>
 *
 * @param qualifiedName the fully qualified name of the element (e.g., "com.example.Order")
 * @since 4.0.0
 */
public record ElementId(String qualifiedName) implements Comparable<ElementId> {

    /**
     * Creates a new ElementId.
     *
     * @param qualifiedName the fully qualified name, must not be null or blank
     * @throws NullPointerException if qualifiedName is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public ElementId {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
    }

    /**
     * Creates an ElementId from a qualified name.
     *
     * @param qualifiedName the fully qualified name (e.g., "com.example.Order")
     * @return a new ElementId
     * @throws NullPointerException if qualifiedName is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public static ElementId of(String qualifiedName) {
        return new ElementId(qualifiedName);
    }

    /**
     * Returns the simple name (class name without package).
     *
     * <p>For nested classes like {@code com.example.Order$OrderLine}, returns {@code OrderLine}.</p>
     *
     * @return the simple name
     */
    public String simpleName() {
        int lastSeparator = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));
        return lastSeparator >= 0 ? qualifiedName.substring(lastSeparator + 1) : qualifiedName;
    }

    /**
     * Returns the package name (everything before the class name).
     *
     * <p>For nested classes like {@code com.example.Order$OrderLine}, returns {@code com.example.Order}
     * (the enclosing type).</p>
     *
     * @return the package name, or empty string if no package
     */
    public String packageName() {
        int lastSeparator = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));
        return lastSeparator >= 0 ? qualifiedName.substring(0, lastSeparator) : "";
    }

    @Override
    public int compareTo(ElementId other) {
        return qualifiedName.compareTo(other.qualifiedName);
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
