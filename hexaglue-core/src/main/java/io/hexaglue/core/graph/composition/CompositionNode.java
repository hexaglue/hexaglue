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

package io.hexaglue.core.graph.composition;

import java.util.Objects;

/**
 * A node in the composition graph representing a domain type.
 *
 * <p>Each node captures the structural characteristics of a domain type that are
 * relevant for classification and relationship analysis. These characteristics
 * are determined through static analysis of the type's definition.
 *
 * <p>Nodes are immutable and identified by their qualified name.
 *
 * @param qualifiedName the fully qualified class name (e.g., "com.example.Order")
 * @param hasIdentity   true if the type has an identity field (id, identifier, etc.)
 * @param isRecord      true if the type is a Java record
 * @param isIdWrapper   true if the type is an ID wrapper (e.g., OrderId)
 * @param simpleName    the simple class name (e.g., "Order")
 * @since 3.0.0
 */
public record CompositionNode(
        String qualifiedName, boolean hasIdentity, boolean isRecord, boolean isIdWrapper, String simpleName) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if qualifiedName or simpleName is null
     */
    public CompositionNode {
        Objects.requireNonNull(qualifiedName, "qualifiedName required");
        Objects.requireNonNull(simpleName, "simpleName required");
    }

    /**
     * Creates a node for a type with identity.
     *
     * @param qualifiedName the fully qualified name
     * @param simpleName    the simple name
     * @return new node with hasIdentity=true
     */
    public static CompositionNode withIdentity(String qualifiedName, String simpleName) {
        return new CompositionNode(qualifiedName, true, false, false, simpleName);
    }

    /**
     * Creates a node for a record type.
     *
     * @param qualifiedName the fully qualified name
     * @param simpleName    the simple name
     * @param hasIdentity   whether the record has an id component
     * @param isIdWrapper   whether the record is an ID wrapper
     * @return new node with isRecord=true
     */
    public static CompositionNode record(
            String qualifiedName, String simpleName, boolean hasIdentity, boolean isIdWrapper) {
        return new CompositionNode(qualifiedName, hasIdentity, true, isIdWrapper, simpleName);
    }

    /**
     * Creates a node for a simple value object.
     *
     * @param qualifiedName the fully qualified name
     * @param simpleName    the simple name
     * @return new node with hasIdentity=false
     */
    public static CompositionNode valueObject(String qualifiedName, String simpleName) {
        return new CompositionNode(qualifiedName, false, false, false, simpleName);
    }

    /**
     * Creates a node for an ID wrapper type.
     *
     * @param qualifiedName the fully qualified name
     * @param simpleName    the simple name
     * @param isRecord      whether it's a record
     * @return new node with isIdWrapper=true
     */
    public static CompositionNode idWrapper(String qualifiedName, String simpleName, boolean isRecord) {
        return new CompositionNode(qualifiedName, true, isRecord, true, simpleName);
    }

    /**
     * Returns true if this node represents a potential entity.
     *
     * <p>A node is a potential entity if it has identity but is not an ID wrapper.
     *
     * @return true if hasIdentity && !isIdWrapper
     */
    public boolean isPotentialEntity() {
        return hasIdentity && !isIdWrapper;
    }

    /**
     * Returns true if this node represents a potential value object.
     *
     * <p>A node is a potential value object if it has no identity or is a record
     * without an id component.
     *
     * @return true if !hasIdentity || (isRecord && !hasIdentity)
     */
    public boolean isPotentialValueObject() {
        return !hasIdentity || (isRecord && !hasIdentity);
    }

    /**
     * Returns a human-readable string representation for debugging.
     *
     * @return formatted string with node characteristics
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(simpleName);
        sb.append(" [");
        if (isIdWrapper) {
            sb.append("ID_WRAPPER");
        } else if (hasIdentity) {
            sb.append("HAS_IDENTITY");
        } else {
            sb.append("NO_IDENTITY");
        }
        if (isRecord) {
            sb.append(",RECORD");
        }
        sb.append("]");
        return sb.toString();
    }
}
