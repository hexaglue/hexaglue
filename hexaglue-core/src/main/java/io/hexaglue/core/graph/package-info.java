/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Application graph model - the central data structure for analysis.
 *
 * <p>The {@link io.hexaglue.core.graph.ApplicationGraph} represents the complete
 * structure of a Java application as a directed graph of nodes (types, fields,
 * methods) and edges (relationships).
 *
 * <h2>Subpackages</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.graph.model} - Node and edge data structures</li>
 *   <li>{@link io.hexaglue.core.graph.builder} - Graph construction from source</li>
 *   <li>{@link io.hexaglue.core.graph.index} - Fast lookup indexes</li>
 *   <li>{@link io.hexaglue.core.graph.query} - Query API for traversal</li>
 *   <li>{@link io.hexaglue.core.graph.style} - Package organization style detection</li>
 * </ul>
 *
 * <h2>Graph Invariants</h2>
 * <ul>
 *   <li><b>G-1</b>: Edge endpoints must exist before the edge is added</li>
 *   <li><b>G-2</b>: Node IDs are unique</li>
 *   <li><b>G-3</b>: The graph is append-only</li>
 * </ul>
 *
 * @see io.hexaglue.core.graph.ApplicationGraph The main graph class
 * @see io.hexaglue.core.graph.builder.GraphBuilder Graph construction
 */
package io.hexaglue.core.graph;
