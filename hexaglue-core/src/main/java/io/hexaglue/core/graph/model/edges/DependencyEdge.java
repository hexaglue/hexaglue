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
 * Represents a type dependency edge with classification of dependency type.
 *
 * <p>Dependency edges capture how types depend on other types through various
 * mechanisms: field types, parameter types, return types, type arguments, etc.
 * This information is critical for:
 * <ul>
 *   <li>Dependency analysis and cycle detection</li>
 *   <li>Impact analysis for refactoring</li>
 *   <li>Understanding coupling between types</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * DependencyEdge edge = DependencyEdge.fieldType(
 *     NodeId.type("com.example.Order"),
 *     NodeId.type("com.example.Customer")
 * );
 *
 * // edge.dependencyType() == DependencyType.FIELD_TYPE
 * // edge.from() == NodeId.type("com.example.Order")
 * // edge.to() == NodeId.type("com.example.Customer")
 * }</pre>
 *
 * @param from the source node (type that depends)
 * @param to the target node (type being depended upon)
 * @param dependencyType the classification of this dependency
 * @since 3.0.0
 */
public record DependencyEdge(NodeId from, NodeId to, DependencyType dependencyType) implements TypedEdge {

    /**
     * Classification of dependency relationships.
     */
    public enum DependencyType {
        /** Field has this type (Order.customer → Customer). */
        FIELD_TYPE(EdgeKind.FIELD_TYPE),

        /** Parameter has this type (void process(Order order) → Order). */
        PARAMETER_TYPE(EdgeKind.PARAMETER_TYPE),

        /** Return type (Order getOrder() → Order). */
        RETURN_TYPE(EdgeKind.RETURN_TYPE),

        /** Type argument (List&lt;Order&gt; → Order). */
        TYPE_ARGUMENT(EdgeKind.TYPE_ARGUMENT),

        /** Extends relationship (OrderImpl extends Order). */
        EXTENDS(EdgeKind.EXTENDS),

        /** Implements relationship (OrderImpl implements Order). */
        IMPLEMENTS(EdgeKind.IMPLEMENTS),

        /** Throws declaration (void process() throws OrderException → OrderException). */
        THROWS(EdgeKind.THROWS),

        /** Annotation usage (@Entity → Entity). */
        ANNOTATED_BY(EdgeKind.ANNOTATED_BY);

        private final EdgeKind edgeKind;

        DependencyType(EdgeKind edgeKind) {
            this.edgeKind = edgeKind;
        }

        /**
         * Returns the corresponding EdgeKind for this dependency type.
         *
         * @return the edge kind
         */
        public EdgeKind edgeKind() {
            return edgeKind;
        }

        /**
         * Returns the DependencyType corresponding to the given EdgeKind.
         *
         * @param kind the edge kind
         * @return the dependency type, or null if not a dependency edge
         */
        public static DependencyType fromEdgeKind(EdgeKind kind) {
            for (DependencyType type : values()) {
                if (type.edgeKind == kind) {
                    return type;
                }
            }
            return null;
        }
    }

    public DependencyEdge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(dependencyType, "dependencyType cannot be null");
    }

    /**
     * Creates a FIELD_TYPE dependency edge.
     *
     * @param from the field node
     * @param to the type node
     * @return the dependency edge
     */
    public static DependencyEdge fieldType(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.FIELD_TYPE);
    }

    /**
     * Creates a PARAMETER_TYPE dependency edge.
     *
     * @param from the method node
     * @param to the type node
     * @return the dependency edge
     */
    public static DependencyEdge parameterType(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.PARAMETER_TYPE);
    }

    /**
     * Creates a RETURN_TYPE dependency edge.
     *
     * @param from the method node
     * @param to the type node
     * @return the dependency edge
     */
    public static DependencyEdge returnType(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.RETURN_TYPE);
    }

    /**
     * Creates a TYPE_ARGUMENT dependency edge.
     *
     * @param from the source node
     * @param to the type argument node
     * @return the dependency edge
     */
    public static DependencyEdge typeArgument(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.TYPE_ARGUMENT);
    }

    /**
     * Creates an EXTENDS dependency edge.
     *
     * @param from the subtype node
     * @param to the supertype node
     * @return the dependency edge
     */
    public static DependencyEdge extends_(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.EXTENDS);
    }

    /**
     * Creates an IMPLEMENTS dependency edge.
     *
     * @param from the implementing type node
     * @param to the interface node
     * @return the dependency edge
     */
    public static DependencyEdge implements_(NodeId from, NodeId to) {
        return new DependencyEdge(from, to, DependencyType.IMPLEMENTS);
    }

    @Override
    public EdgeKind kind() {
        return dependencyType.edgeKind();
    }

    @Override
    public Map<String, Object> metadata() {
        return Map.of("dependencyType", dependencyType.name());
    }

    @Override
    public Edge toEdge() {
        return Edge.raw(from, to, kind());
    }

    /**
     * Creates a DependencyEdge from a basic Edge.
     *
     * @param edge the basic edge
     * @return the dependency edge, or null if the edge kind is not a dependency
     */
    public static DependencyEdge fromEdge(Edge edge) {
        DependencyType type = DependencyType.fromEdgeKind(edge.kind());
        return type != null ? new DependencyEdge(edge.from(), edge.to(), type) : null;
    }
}
