/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * High-level query API for the application graph.
 *
 * <p>The {@link io.hexaglue.core.graph.query.GraphQuery} interface provides
 * convenient methods for traversing and querying the graph, combining
 * index lookups with node access.
 *
 * <h2>Query Examples</h2>
 * <pre>{@code
 * GraphQuery query = graph.query();
 *
 * // Find types
 * Optional<TypeNode> order = query.type("com.example.Order");
 * Stream<TypeNode> interfaces = query.types(t -> t.isInterface());
 *
 * // Traverse relationships
 * List<TypeNode> implementors = query.implementorsOf(interface);
 * List<FieldNode> fields = query.fieldsOf(type);
 * }</pre>
 *
 * @see io.hexaglue.core.graph.query.GraphQuery The query interface
 * @see io.hexaglue.core.graph.query.DefaultGraphQuery Default implementation
 */
package io.hexaglue.core.graph.query;
