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

/**
 * Represents the certainty level of a classification decision.
 *
 * <p>This enum quantifies how confident HexaGlue is about a classification decision.
 * Higher certainty means the classification is based on stronger signals and is more
 * reliable. Plugins can use this to adjust behavior (e.g., skip generation for uncertain
 * classifications or require manual review).
 *
 * <p>The certainty level is determined by combining multiple classification signals:
 * <ul>
 *   <li><b>EXPLICIT</b>: User explicitly declared intent via annotations</li>
 *   <li><b>CERTAIN_BY_STRUCTURE</b>: Deterministic structural rules matched</li>
 *   <li><b>INFERRED</b>: Graph-based inference from relationships</li>
 *   <li><b>UNCERTAIN</b>: Weak signals, conflicting evidence</li>
 *   <li><b>NONE</b>: No classification possible</li>
 * </ul>
 *
 * @since 3.0.0
 */
public enum CertaintyLevel {

    /**
     * Classification based on explicit user annotation.
     *
     * <p>Highest certainty - the developer explicitly declared intent using
     * HexaGlue annotations (e.g., {@code @AggregateRoot}, {@code @Entity},
     * {@code @ValueObject}).
     *
     * <p>Example: Class annotated with {@code @AggregateRoot}
     */
    EXPLICIT,

    /**
     * Classification based on deterministic structural analysis.
     *
     * <p>Very high certainty - the classification follows from clear, unambiguous
     * structural patterns that are widely accepted in DDD (e.g., Repository<T> pattern,
     * Java records for value objects).
     *
     * <p>Example: Class that is the type parameter of a Repository interface
     */
    CERTAIN_BY_STRUCTURE,

    /**
     * Classification inferred from graph analysis and relationships.
     *
     * <p>Medium-high certainty - the classification is inferred from the type's
     * position in the domain model graph and its relationships to other classified
     * types.
     *
     * <p>Example: Type embedded in an aggregate root is inferred to be a value object
     */
    INFERRED,

    /**
     * Classification is uncertain and needs manual review.
     *
     * <p>Low certainty - signals are weak or conflicting. The classification might
     * be wrong and should be verified by the developer.
     *
     * <p>Example: Type with identity field but no clear aggregate root characteristics
     */
    UNCERTAIN,

    /**
     * No classification could be determined.
     *
     * <p>Zero certainty - the classifier found no evidence to assign a domain kind.
     * This typically happens for types that are not part of the domain model (DTOs,
     * infrastructure classes, etc.).
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
