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

package io.hexaglue.core.classification;

/**
 * Status of a classification attempt.
 */
public enum ClassificationStatus {

    /**
     * Type was successfully classified with a single winning criteria.
     */
    CLASSIFIED,

    /**
     * No criteria matched - type remains unclassified.
     */
    UNCLASSIFIED,

    /**
     * Multiple conflicting criteria matched - classification is ambiguous.
     */
    CONFLICT
}
