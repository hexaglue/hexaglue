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

import io.hexaglue.arch.ElementId;
import java.util.Objects;

/**
 * Stable identifier for an architectural type.
 *
 * <p>This is the equivalent of {@link ElementId} for the new {@link ArchType} hierarchy.
 * It provides bidirectional conversion to/from ElementId for migration purposes.</p>
 *
 * <h2>Design Rationale</h2>
 * <p>The identity of a type does NOT depend on its classification. If heuristics evolve
 * and {@code Order} is reclassified from ENTITY to AGGREGATE_ROOT, the TypeId remains
 * stable: {@code "com.example.Order"}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * TypeId id = TypeId.of("com.example.Order");
 * assert id.simpleName().equals("Order");
 * assert id.packageName().equals("com.example");
 *
 * // Conversion from/to ElementId
 * ElementId elementId = id.toElementId();
 * TypeId back = TypeId.fromElementId(elementId);
 * }</pre>
 *
 * @param qualifiedName the fully qualified name of the type (e.g., "com.example.Order")
 * @since 4.1.0
 */
public record TypeId(String qualifiedName) implements Comparable<TypeId> {

    /**
     * Creates a new TypeId.
     *
     * @param qualifiedName the fully qualified name, must not be null or blank
     * @throws NullPointerException if qualifiedName is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public TypeId {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
    }

    /**
     * Creates a TypeId from a qualified name.
     *
     * @param qualifiedName the fully qualified name (e.g., "com.example.Order")
     * @return a new TypeId
     * @throws NullPointerException if qualifiedName is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    public static TypeId of(String qualifiedName) {
        return new TypeId(qualifiedName);
    }

    /**
     * Creates a TypeId from an {@link ElementId}.
     *
     * @param id the element id to convert
     * @return a new TypeId with the same qualified name
     * @throws NullPointerException if id is null
     */
    public static TypeId fromElementId(ElementId id) {
        Objects.requireNonNull(id, "id must not be null");
        return new TypeId(id.qualifiedName());
    }

    /**
     * Converts this TypeId to an {@link ElementId}.
     *
     * @return an ElementId with the same qualified name
     */
    public ElementId toElementId() {
        return ElementId.of(qualifiedName);
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
    public int compareTo(TypeId other) {
        return qualifiedName.compareTo(other.qualifiedName);
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
