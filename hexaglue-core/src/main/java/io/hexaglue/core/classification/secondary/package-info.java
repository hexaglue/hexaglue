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
 * Secondary classification framework for user-provided classifiers.
 *
 * <p>This package provides the execution infrastructure for running user-provided
 * secondary classifiers that can override or refine primary classification results.
 *
 * <h2>Architecture</h2>
 * <p>Secondary classification runs AFTER primary classification completes:
 * <pre>
 * Primary Classification (Deterministic)
 *          ↓
 * Secondary Classification (User-Provided)
 *          ↓
 * Final Classification Result
 * </pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.classification.secondary.SecondaryClassifierExecutor}
 *       - Executes classifiers with timeout protection</li>
 *   <li>{@link io.hexaglue.core.classification.secondary.WeightedMultiSignalClassifier}
 *       - Example multi-signal scoring classifier</li>
 * </ul>
 *
 * <h2>Execution Guarantees</h2>
 * <ul>
 *   <li>Each classifier runs within its configured timeout</li>
 *   <li>Exceptions are caught and logged without failing the process</li>
 *   <li>Classifiers are isolated in separate threads</li>
 *   <li>Primary results are used as fallback on timeout or error</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.core.classification.secondary;
