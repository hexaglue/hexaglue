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

import java.util.Objects;

/**
 * Information about a classification conflict.
 *
 * <p>When multiple classification criteria match with different suggestions,
 * this record captures the alternative classification that was considered
 * but not selected.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ConflictInfo conflict = new ConflictInfo(
 *     ElementKind.ENTITY,
 *     "Type also has identity field suggesting entity",
 *     ConfidenceLevel.MEDIUM
 * );
 * }</pre>
 *
 * @param alternativeKind the alternative element kind that was considered
 * @param reason explanation of why this alternative was considered
 * @param alternativeConfidence the confidence level of the alternative classification
 * @since 4.0.0
 */
public record ConflictInfo(ElementKind alternativeKind, String reason, ConfidenceLevel alternativeConfidence) {

    /**
     * Creates a new ConflictInfo instance.
     *
     * @param alternativeKind the alternative kind, must not be null
     * @param reason the reason, must not be null or blank
     * @param alternativeConfidence the confidence level, must not be null
     * @throws NullPointerException if any field is null
     * @throws IllegalArgumentException if reason is blank
     */
    public ConflictInfo {
        Objects.requireNonNull(alternativeKind, "alternativeKind must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(alternativeConfidence, "alternativeConfidence must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
