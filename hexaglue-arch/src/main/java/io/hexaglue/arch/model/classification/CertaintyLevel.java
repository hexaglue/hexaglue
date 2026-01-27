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

package io.hexaglue.arch.model.classification;

/**
 * Represents the confidence level of a classification decision.
 *
 * <p>Certainty levels form a hierarchy from highest (EXPLICIT) to lowest (NONE),
 * helping users understand how reliable a classification is and whether it might
 * need manual review.
 *
 * <p>When multiple strategies yield different results, the one with higher certainty
 * typically takes precedence.
 *
 * @since 5.0.0
 */
public enum CertaintyLevel {

    /**
     * Classification was explicitly declared by the developer via annotations.
     *
     * <p>This is the highest confidence level because it represents direct developer intent.
     * Examples include {@code @AggregateRoot}, {@code @Entity}, {@code @ValueObject}.
     */
    EXPLICIT,

    /**
     * Classification was determined by structural patterns that are highly reliable.
     *
     * <p>Examples include:
     * <ul>
     *   <li>Repository type parameter → aggregate root</li>
     *   <li>Java record → value object</li>
     *   <li>Interface extending a known port type</li>
     * </ul>
     */
    CERTAIN_BY_STRUCTURE,

    /**
     * Classification was inferred from relationships and context.
     *
     * <p>Examples include:
     * <ul>
     *   <li>Type embedded in an aggregate root → value object</li>
     *   <li>Type referenced by multiple aggregates → shared value object</li>
     * </ul>
     */
    INFERRED,

    /**
     * Classification was made but with low confidence.
     *
     * <p>The classifier found some signals but they were weak or conflicting.
     * These classifications may benefit from manual review.
     */
    UNCERTAIN,

    /**
     * No classification could be made.
     *
     * <p>The type did not match any known patterns or the signals were too weak
     * to make even an uncertain classification.
     */
    NONE;

    /**
     * Returns true if this certainty level is reliable enough for automatic code generation.
     *
     * <p>Only {@code EXPLICIT} and {@code CERTAIN_BY_STRUCTURE} are considered
     * reliable by default. Plugins may choose to be more or less conservative.
     *
     * @return true if certainty is EXPLICIT or CERTAIN_BY_STRUCTURE
     */
    public boolean isReliable() {
        return this == EXPLICIT || this == CERTAIN_BY_STRUCTURE;
    }

    /**
     * Returns true if this certainty level requires manual review.
     *
     * <p>Classifications with {@code UNCERTAIN} or {@code NONE} should be reviewed
     * by developers before relying on them.
     *
     * @return true if certainty is UNCERTAIN or NONE
     */
    public boolean needsReview() {
        return this == UNCERTAIN || this == NONE;
    }

    /**
     * Returns true if this certainty level is higher than or equal to the given level.
     *
     * <p>Certainty ordering: EXPLICIT > CERTAIN_BY_STRUCTURE > INFERRED > UNCERTAIN > NONE
     *
     * @param other the level to compare against
     * @return true if this level is higher or equal
     */
    public boolean isAtLeast(CertaintyLevel other) {
        return this.ordinal() <= other.ordinal();
    }
}
