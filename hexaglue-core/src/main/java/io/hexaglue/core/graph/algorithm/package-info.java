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
 * Graph algorithms for dependency analysis and cycle detection.
 *
 * <p>This package provides generic, reusable graph algorithms:
 * <ul>
 *   <li>{@link io.hexaglue.core.graph.algorithm.TarjanCycleDetector} - Tarjan's SCC algorithm for cycle detection</li>
 *   <li>{@link io.hexaglue.core.graph.algorithm.Cycle} - Represents a detected cycle</li>
 *   <li>{@link io.hexaglue.core.graph.algorithm.CycleDetectionConfig} - Configuration for cycle detection</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.core.graph.algorithm;
