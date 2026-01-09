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
 * Composition graph data structures for deterministic domain classification.
 *
 * <p>This package provides the core graph representation used by the deterministic classifier
 * to analyze relationships between domain types. The composition graph captures:
 * <ul>
 *   <li>Composition relationships (aggregate boundaries)</li>
 *   <li>Reference-by-ID relationships (cross-aggregate references)</li>
 *   <li>Direct references (design smells)</li>
 * </ul>
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link io.hexaglue.core.graph.composition.CompositionGraph} - The main graph structure</li>
 *   <li>{@link io.hexaglue.core.graph.composition.CompositionNode} - Represents a domain type</li>
 *   <li>{@link io.hexaglue.core.graph.composition.CompositionEdge} - Represents a relationship</li>
 *   <li>{@link io.hexaglue.core.graph.composition.RelationType} - Type of relationship</li>
 *   <li>{@link io.hexaglue.core.graph.composition.Cardinality} - One or many</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.core.graph.composition;
