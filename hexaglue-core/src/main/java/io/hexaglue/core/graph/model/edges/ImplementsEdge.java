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

/**
 * Represents an implements edge with direct/transitive flag.
 *
 * <p>Implements edges capture type-to-interface relationships, which are critical for:
 * <ul>
 *   <li>Identifying ports in hexagonal architecture (driven/driving interfaces)</li>
 *   <li>Analyzing abstraction patterns and dependency inversion</li>
 *   <li>Understanding contract-based design</li>
 *   <li>Computing transitive interface dependencies</li>
 * </ul>
 *
 * <p>Direct vs Transitive:
 * <ul>
 *   <li><b>Direct:</b> OrderService implements UseCase (explicitly declared)</li>
 *   <li><b>Transitive:</b> OrderService → UseCase → Executable (inherited interface)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ImplementsEdge edge = ImplementsEdge.direct(
 *     NodeId.type("com.example.OrderService"),
 *     NodeId.type("com.example.UseCase")
 * );
 *
 * // edge.isDirect() == true
 * // edge.isTransitive() == false
 * }</pre>
 *
 * @param from the implementing type node
 * @param to the interface node
 * @param isDirect true if this is a directly declared interface, false if inherited
 * @since 3.0.0
 */
public record ImplementsEdge(NodeId from, NodeId to, boolean isDirect) implements TypedEdge {

    public ImplementsEdge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
    }

    /**
     * Creates a direct implements edge (explicitly declared interface).
     *
     * @param implementor the implementing type node
     * @param interfaceType the interface node
     * @return the implements edge
     */
    public static ImplementsEdge direct(NodeId implementor, NodeId interfaceType) {
        return new ImplementsEdge(implementor, interfaceType, true);
    }

    /**
     * Creates a transitive implements edge (inherited interface).
     *
     * @param implementor the implementing type node
     * @param inheritedInterface the inherited interface node
     * @return the implements edge
     */
    public static ImplementsEdge transitive(NodeId implementor, NodeId inheritedInterface) {
        return new ImplementsEdge(implementor, inheritedInterface, false);
    }

    @Override
    public EdgeKind kind() {
        return EdgeKind.IMPLEMENTS;
    }

    @Override
    public Map<String, Object> metadata() {
        return Map.of("isDirect", isDirect, "isTransitive", !isDirect);
    }

    @Override
    public Edge toEdge() {
        return Edge.raw(from, to, EdgeKind.IMPLEMENTS);
    }

    /**
     * Returns true if this is a direct implements relationship.
     *
     * @return true if explicitly declared
     */
    public boolean isDirect() {
        return isDirect;
    }

    /**
     * Returns true if this is a transitive implements relationship.
     *
     * @return true if inherited through parent class or interface
     */
    public boolean isTransitive() {
        return !isDirect;
    }

    /**
     * Creates an ImplementsEdge from a basic Edge.
     *
     * <p>Assumes the edge is direct unless proven otherwise through analysis.
     *
     * @param edge the basic edge
     * @return the implements edge, or null if not an IMPLEMENTS edge
     */
    public static ImplementsEdge fromEdge(Edge edge) {
        if (edge.kind() != EdgeKind.IMPLEMENTS) {
            return null;
        }
        // Default to direct - transitive edges must be computed separately
        return new ImplementsEdge(edge.from(), edge.to(), true);
    }
}
