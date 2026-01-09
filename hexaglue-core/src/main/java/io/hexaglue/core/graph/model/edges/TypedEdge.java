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

import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.NodeId;
import java.util.Map;

/**
 * Sealed interface for typed edges with rich metadata.
 *
 * <p>This interface provides a type-safe hierarchy of edges that extends the basic
 * {@link io.hexaglue.core.graph.model.Edge} model with domain-specific information.
 * Each edge subtype carries specific metadata relevant to its relationship kind.
 *
 * <p>Edge subtypes:
 * <ul>
 *   <li>{@link DependencyEdge} - generic type dependencies with dependency type classification</li>
 *   <li>{@link MethodCallEdge} - method invocations with calling context and frequency</li>
 *   <li>{@link FieldAccessEdge} - field access with read/write classification</li>
 *   <li>{@link InheritanceEdge} - extends relationships with direct/transitive flag</li>
 *   <li>{@link ImplementsEdge} - implements relationships with direct/transitive flag</li>
 * </ul>
 *
 * <p>All typed edges can be converted to/from the basic Edge representation for
 * compatibility with the existing graph infrastructure.
 *
 * @since 3.0.0
 */
public sealed interface TypedEdge
        permits DependencyEdge, MethodCallEdge, FieldAccessEdge, InheritanceEdge, ImplementsEdge {

    /**
     * Returns the source node of this edge.
     *
     * @return the source node id
     */
    NodeId from();

    /**
     * Returns the target node of this edge.
     *
     * @return the target node id
     */
    NodeId to();

    /**
     * Returns the kind of this edge.
     *
     * @return the edge kind
     */
    EdgeKind kind();

    /**
     * Returns the metadata associated with this edge.
     *
     * <p>The metadata map contains type-specific information that varies by edge subtype.
     * Keys and values are defined by each subtype.
     *
     * @return an immutable map of metadata
     */
    Map<String, Object> metadata();

    /**
     * Converts this typed edge to a basic Edge for graph storage.
     *
     * <p>This method enables interoperability with the existing graph infrastructure
     * which uses the basic Edge record for storage and indexing.
     *
     * @return the basic edge representation
     */
    io.hexaglue.core.graph.model.Edge toEdge();
}
