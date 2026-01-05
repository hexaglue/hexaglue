/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Graph indexes for fast lookups.
 *
 * <p>The {@link io.hexaglue.core.graph.index.GraphIndexes} class maintains
 * indexes that are built incrementally as nodes and edges are added to the graph.
 *
 * <h2>Primary Indexes</h2>
 * <ul>
 *   <li>Types by package name</li>
 *   <li>Types by JavaForm (CLASS, INTERFACE, RECORD, etc.)</li>
 *   <li>Elements by annotation</li>
 * </ul>
 *
 * <h2>Relationship Indexes</h2>
 * <ul>
 *   <li>Type → declared members</li>
 *   <li>Type → subtypes / supertypes</li>
 *   <li>Interface → implementors</li>
 *   <li>Type → interfaces using in signature</li>
 * </ul>
 *
 * @see io.hexaglue.core.graph.index.GraphIndexes The main index class
 */
package io.hexaglue.core.graph.index;
