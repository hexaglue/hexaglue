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

import java.util.Objects;

/**
 * Represents a conflict between classification criteria.
 *
 * <p>When multiple criteria match with different target kinds,
 * a conflict is recorded for debugging and traceability.
 *
 * @param competingKind the kind that competed with the winner
 * @param competingCriteria name of the competing criteria
 * @param competingConfidence confidence of the competing criteria
 * @param competingPriority priority of the competing criteria
 * @param rationale explanation of why this is a conflict
 */
public record Conflict(
        String competingKind,
        String competingCriteria,
        ConfidenceLevel competingConfidence,
        int competingPriority,
        String rationale) {

    public Conflict {
        Objects.requireNonNull(competingKind, "competingKind cannot be null");
        Objects.requireNonNull(competingCriteria, "competingCriteria cannot be null");
        Objects.requireNonNull(competingConfidence, "competingConfidence cannot be null");
        Objects.requireNonNull(rationale, "rationale cannot be null");
    }

    /**
     * Creates a conflict indicating that two criteria matched for different kinds.
     */
    public static Conflict between(
            String kind, String criteria, ConfidenceLevel confidence, int priority, String winnerKind) {
        return new Conflict(
                kind, criteria, confidence, priority, "Matched as %s but winner is %s".formatted(kind, winnerKind));
    }
}
