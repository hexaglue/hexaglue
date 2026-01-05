/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Graph construction from source code.
 *
 * <p>The {@link io.hexaglue.core.graph.builder.GraphBuilder} transforms a
 * {@link io.hexaglue.core.frontend.JavaSemanticModel} into an
 * {@link io.hexaglue.core.graph.ApplicationGraph}.
 *
 * <h2>Build Phases</h2>
 * <ol>
 *   <li><b>Pass 1</b>: Create TypeNodes from source types</li>
 *   <li><b>Pass 1.5</b>: Detect package organization style</li>
 *   <li><b>Pass 2</b>: Create member nodes and RAW edges</li>
 *   <li><b>Pass 3</b>: Compute DERIVED edges</li>
 * </ol>
 *
 * @see io.hexaglue.core.graph.builder.GraphBuilder Main builder class
 * @see io.hexaglue.core.graph.builder.DerivedEdgeComputer DERIVED edge computation
 */
package io.hexaglue.core.graph.builder;
