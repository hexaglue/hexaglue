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

package io.hexaglue.core.graph.model.edges;

import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.NodeId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an inheritance edge (extends relationship) with direct/transitive flag.
 *
 * <p>Inheritance edges capture class-to-superclass relationships, which are essential for:
 * <ul>
 *   <li>Analyzing type hierarchies</li>
 *   <li>Detecting deep inheritance chains (code smell)</li>
 *   <li>Understanding polymorphic relationships</li>
 *   <li>Computing transitive dependencies</li>
 * </ul>
 *
 * <p>Direct vs Transitive:
 * <ul>
 *   <li><b>Direct:</b> OrderImpl extends AbstractOrder (direct parent)</li>
 *   <li><b>Transitive:</b> OrderImpl → AbstractOrder → BaseEntity (grandparent)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * InheritanceEdge edge = InheritanceEdge.direct(
 *     NodeId.type("com.example.OrderImpl"),
 *     NodeId.type("com.example.AbstractOrder")
 * );
 *
 * // edge.isDirect() == true
 * // edge.isTransitive() == false
 * }</pre>
 *
 * @param from the subtype node
 * @param to the supertype node
 * @param isDirect true if this is a direct parent (immediate superclass), false if transitive
 * @since 3.0.0
 */
public record InheritanceEdge(NodeId from, NodeId to, boolean isDirect) implements TypedEdge {

    public InheritanceEdge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
    }

    /**
     * Creates a direct inheritance edge (immediate parent).
     *
     * @param subtype the subtype node
     * @param supertype the supertype node
     * @return the inheritance edge
     */
    public static InheritanceEdge direct(NodeId subtype, NodeId supertype) {
        return new InheritanceEdge(subtype, supertype, true);
    }

    /**
     * Creates a transitive inheritance edge (ancestor, not immediate parent).
     *
     * @param subtype the subtype node
     * @param ancestor the ancestor node
     * @return the inheritance edge
     */
    public static InheritanceEdge transitive(NodeId subtype, NodeId ancestor) {
        return new InheritanceEdge(subtype, ancestor, false);
    }

    @Override
    public EdgeKind kind() {
        return EdgeKind.EXTENDS;
    }

    @Override
    public Map<String, Object> metadata() {
        return Map.of("isDirect", isDirect, "isTransitive", !isDirect);
    }

    @Override
    public Edge toEdge() {
        return Edge.raw(from, to, EdgeKind.EXTENDS);
    }

    /**
     * Returns true if this is a direct inheritance relationship.
     *
     * @return true if immediate parent
     */
    public boolean isDirect() {
        return isDirect;
    }

    /**
     * Returns true if this is a transitive inheritance relationship.
     *
     * @return true if ancestor but not immediate parent
     */
    public boolean isTransitive() {
        return !isDirect;
    }

    /**
     * Creates an InheritanceEdge from a basic Edge.
     *
     * <p>Assumes the edge is direct unless proven otherwise through analysis.
     *
     * @param edge the basic edge
     * @return the inheritance edge, or empty if not an EXTENDS edge
     */
    public static Optional<InheritanceEdge> fromEdge(Edge edge) {
        if (edge.kind() != EdgeKind.EXTENDS) {
            return Optional.empty();
        }
        // Default to direct - transitive edges must be computed separately
        return Optional.of(new InheritanceEdge(edge.from(), edge.to(), true));
    }
}
