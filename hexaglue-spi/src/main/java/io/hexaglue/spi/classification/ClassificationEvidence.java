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

package io.hexaglue.spi.classification;

import java.util.Objects;

/**
 * Represents a single piece of evidence supporting a classification decision.
 *
 * <p>Classification decisions are based on multiple signals (evidence) that are
 * weighted and combined to determine both the domain kind and certainty level.
 * This record captures individual signals for transparency and debuggability.
 *
 * <p>Evidence is collected during the classification process and attached to
 * classified types, enabling developers to understand why a particular classification
 * was chosen and to tune the classifier if needed.
 *
 * <p>Example evidence for classifying a type as AGGREGATE_ROOT:
 * <pre>{@code
 * new ClassificationEvidence(
 *     "REPOSITORY_MANAGED",
 *     100,
 *     "Type is managed by OrderRepository"
 * )
 * }</pre>
 *
 * @param signal      the signal identifier (e.g., "REPOSITORY_MANAGED", "HAS_IDENTITY")
 * @param weight      the weight/confidence of this signal (higher = stronger)
 * @param description human-readable explanation of this signal
 * @since 3.0.0
 */
public record ClassificationEvidence(String signal, int weight, String description) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if signal or description is null
     */
    public ClassificationEvidence {
        Objects.requireNonNull(signal, "signal required");
        Objects.requireNonNull(description, "description required");
    }

    /**
     * Creates evidence with a positive signal.
     *
     * @param signal      the signal identifier
     * @param weight      the weight (should be positive)
     * @param description the explanation
     * @return new evidence instance
     */
    public static ClassificationEvidence positive(String signal, int weight, String description) {
        return new ClassificationEvidence(signal, weight, description);
    }

    /**
     * Creates evidence with a negative signal (contra-indication).
     *
     * @param signal      the signal identifier
     * @param weight      the weight (will be negative)
     * @param description the explanation
     * @return new evidence instance
     */
    public static ClassificationEvidence negative(String signal, int weight, String description) {
        return new ClassificationEvidence(signal, -Math.abs(weight), description);
    }

    /**
     * Returns true if this evidence supports the classification (positive weight).
     *
     * @return true if weight is positive
     */
    public boolean isPositive() {
        return weight > 0;
    }

    /**
     * Returns true if this evidence contradicts the classification (negative weight).
     *
     * @return true if weight is negative
     */
    public boolean isNegative() {
        return weight < 0;
    }

    /**
     * Returns the absolute weight value.
     *
     * @return absolute value of weight
     */
    public int absoluteWeight() {
        return Math.abs(weight);
    }
}
