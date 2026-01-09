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
 * Types related to domain classification and confidence tracking.
 *
 * <p>This package provides the foundation for understanding how HexaGlue
 * classifies types into DDD tactical patterns. It captures both the certainty
 * of a classification decision and the strategy used to arrive at it.
 *
 * <h2>Classification Architecture</h2>
 * <p>HexaGlue uses a two-phase classification approach:
 * <ol>
 *   <li><b>Primary Classification</b>: Deterministic rules and structural patterns
 *       applied by the HexaGlue engine</li>
 *   <li><b>Secondary Classification</b>: User-provided classifiers that can
 *       override or refine primary results</li>
 * </ol>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Certainty Level</b>: How confident is the classification?</li>
 *   <li><b>Classification Strategy</b>: What evidence led to this classification?</li>
 *   <li><b>Classification Evidence</b>: Individual signals that support the decision</li>
 *   <li><b>HexaglueClassifier</b>: User-provided secondary classifier interface</li>
 *   <li><b>Classification Context</b>: State available to secondary classifiers</li>
 * </ul>
 *
 * <p>Together, these elements provide transparency and traceability for
 * classification decisions, enabling developers to understand, debug, and
 * tune the classifier's behavior.
 *
 * @since 3.0.0
 */
package io.hexaglue.spi.classification;
