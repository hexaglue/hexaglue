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

package io.hexaglue.arch;

/**
 * Confidence level of a classification decision.
 *
 * <p>Represents how certain the classifier is about its decision.
 * Higher confidence means more evidence supports the classification.</p>
 *
 * <h2>Levels</h2>
 * <ul>
 *   <li>{@link #HIGH} - Explicit annotation or strong convention match</li>
 *   <li>{@link #MEDIUM} - Heuristic match with supporting evidence</li>
 *   <li>{@link #LOW} - Weak match or conflicting evidence</li>
 * </ul>
 *
 * @since 4.0.0
 */
public enum ConfidenceLevel {

    /**
     * High confidence: explicit annotation or strong convention match.
     *
     * <p>Examples: @AggregateRoot annotation, explicit configuration.</p>
     */
    HIGH,

    /**
     * Medium confidence: heuristic match with supporting evidence.
     *
     * <p>Examples: naming convention + structure match.</p>
     */
    MEDIUM,

    /**
     * Low confidence: weak match or conflicting evidence.
     *
     * <p>Examples: only naming convention, conflicting criteria.</p>
     */
    LOW
}
