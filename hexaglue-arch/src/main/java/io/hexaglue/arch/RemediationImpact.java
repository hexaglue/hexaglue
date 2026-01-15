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
 * Impact of a remediation action on classification.
 *
 * <p>Describes what classification result will be achieved if a
 * remediation action is applied.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * RemediationImpact impact = RemediationImpact.explicit(ElementKind.AGGREGATE_ROOT);
 * // Will result in AGGREGATE_ROOT with HIGH confidence
 * }</pre>
 *
 * @param resultingKind the element kind that will result from the action
 * @param resultingConfidence the confidence level that will result
 * @param description human-readable description of the impact
 * @since 4.0.0
 */
public record RemediationImpact(ElementKind resultingKind, ConfidenceLevel resultingConfidence, String description) {

    /**
     * Creates a new RemediationImpact instance.
     *
     * @param resultingKind the resulting kind, must not be null
     * @param resultingConfidence the resulting confidence, must not be null
     * @param description the description, must not be null
     * @throws NullPointerException if any field is null
     */
    public RemediationImpact {
        Objects.requireNonNull(resultingKind, "resultingKind must not be null");
        Objects.requireNonNull(resultingConfidence, "resultingConfidence must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }

    /**
     * Creates an impact for an explicit classification (HIGH confidence).
     *
     * <p>Used when the action will result in an explicit, unambiguous classification.</p>
     *
     * @param kind the element kind
     * @return a new RemediationImpact with HIGH confidence
     */
    public static RemediationImpact explicit(ElementKind kind) {
        return new RemediationImpact(
                kind, ConfidenceLevel.HIGH, "Will be classified as " + kind + " with HIGH confidence");
    }

    /**
     * Creates an impact for an improved classification.
     *
     * <p>Used when the action will improve the confidence level but may not
     * make it explicit.</p>
     *
     * @param kind the element kind
     * @param level the resulting confidence level
     * @return a new RemediationImpact
     */
    public static RemediationImpact improved(ElementKind kind, ConfidenceLevel level) {
        return new RemediationImpact(kind, level, "Will improve confidence to " + level);
    }
}
