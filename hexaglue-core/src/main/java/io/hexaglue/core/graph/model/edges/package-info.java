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

/**
 * Typed edge hierarchy with rich metadata for graph relationships.
 *
 * <p>This package provides a sealed type hierarchy for edges that extends the basic
 * {@link io.hexaglue.core.graph.model.Edge} model with domain-specific metadata:
 *
 * <ul>
 *   <li>{@link io.hexaglue.core.graph.model.edges.DependencyEdge} - type dependencies with classification</li>
 *   <li>{@link io.hexaglue.core.graph.model.edges.MethodCallEdge} - method invocations with calling context</li>
 *   <li>{@link io.hexaglue.core.graph.model.edges.FieldAccessEdge} - field access with read/write tracking</li>
 *   <li>{@link io.hexaglue.core.graph.model.edges.InheritanceEdge} - extends relationships with direct/transitive flag</li>
 *   <li>{@link io.hexaglue.core.graph.model.edges.ImplementsEdge} - implements relationships with direct/transitive flag</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * <p><b>Type Safety:</b> Sealed interface hierarchy ensures exhaustive pattern matching
 * and prevents invalid edge subtypes.
 *
 * <p><b>Interoperability:</b> All typed edges can convert to/from the basic Edge representation
 * for compatibility with existing graph infrastructure.
 *
 * <p><b>Rich Metadata:</b> Each edge subtype carries relevant metadata in a type-safe manner,
 * avoiding stringly-typed map lookups.
 *
 * <p><b>Immutability:</b> All edge types are immutable records, ensuring thread-safety
 * and preventing accidental mutation.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a method call edge
 * MethodCallEdge call = MethodCallEdge.create(
 *     NodeId.method("com.example.UseCase", "execute", "Order"),
 *     NodeId.method("com.example.Repository", "save", "Order")
 * );
 *
 * // Convert to basic edge for graph storage
 * Edge edge = call.toEdge();
 * graph.addEdge(edge);
 *
 * // Query with type safety
 * if (call.isInstanceCall()) {
 *     System.out.println("Instance method call with " + call.invocationCount() + " invocations");
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
package io.hexaglue.core.graph.model.edges;
