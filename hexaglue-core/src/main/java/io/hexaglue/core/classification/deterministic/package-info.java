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
 * Deterministic domain type classification engine.
 *
 * <p>This package provides the main deterministic classifier that combines:
 * <ul>
 *   <li>Explicit annotations</li>
 *   <li>Repository pattern detection</li>
 *   <li>Record-based value object detection</li>
 *   <li>ID wrapper detection</li>
 *   <li>Composition graph analysis</li>
 *   <li>Anomaly detection</li>
 * </ul>
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link io.hexaglue.core.classification.deterministic.DeterministicClassifier} - Main classifier</li>
 *   <li>{@link io.hexaglue.core.classification.deterministic.Classification} - Classification decision</li>
 *   <li>{@link io.hexaglue.core.classification.deterministic.ClassificationResult} - Complete result</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.core.classification.deterministic;
