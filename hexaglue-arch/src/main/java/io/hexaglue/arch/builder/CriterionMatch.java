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

package io.hexaglue.arch.builder;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.Evidence;
import java.util.List;
import java.util.Objects;

/**
 * The result of a classification criterion match.
 *
 * <p>Contains the justification for the match, the confidence level,
 * and the evidence supporting the match.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * CriterionMatch match = CriterionMatch.of(
 *     "Type has @AggregateRoot annotation",
 *     ConfidenceLevel.HIGH,
 *     Evidence.of(EvidenceType.ANNOTATION, "@AggregateRoot annotation found")
 * );
 * }</pre>
 *
 * @param justification human-readable explanation of why the criterion matched
 * @param confidence the confidence level of the match
 * @param evidence the list of evidence supporting the match
 * @since 4.0.0
 */
public record CriterionMatch(String justification, ConfidenceLevel confidence, List<Evidence> evidence) {

    /**
     * Creates a new CriterionMatch instance.
     *
     * @param justification the justification, must not be null or blank
     * @param confidence the confidence level, must not be null
     * @param evidence the evidence list, must not be null
     * @throws NullPointerException if any required field is null
     * @throws IllegalArgumentException if justification is blank
     */
    public CriterionMatch {
        Objects.requireNonNull(justification, "justification must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification must not be blank");
        }
        evidence = List.copyOf(evidence);
    }

    /**
     * Creates a CriterionMatch with a single piece of evidence.
     *
     * @param justification the justification
     * @param confidence the confidence level
     * @param evidence the single evidence
     * @return a new CriterionMatch
     */
    public static CriterionMatch of(String justification, ConfidenceLevel confidence, Evidence evidence) {
        return new CriterionMatch(justification, confidence, List.of(evidence));
    }

    /**
     * Creates a CriterionMatch with multiple pieces of evidence.
     *
     * @param justification the justification
     * @param confidence the confidence level
     * @param evidence the evidence list
     * @return a new CriterionMatch
     */
    public static CriterionMatch of(String justification, ConfidenceLevel confidence, List<Evidence> evidence) {
        return new CriterionMatch(justification, confidence, evidence);
    }

    /**
     * Creates a HIGH confidence match with a single evidence.
     *
     * @param justification the justification
     * @param evidence the evidence
     * @return a new CriterionMatch with HIGH confidence
     */
    public static CriterionMatch high(String justification, Evidence evidence) {
        return new CriterionMatch(justification, ConfidenceLevel.HIGH, List.of(evidence));
    }

    /**
     * Creates a MEDIUM confidence match with a single evidence.
     *
     * @param justification the justification
     * @param evidence the evidence
     * @return a new CriterionMatch with MEDIUM confidence
     */
    public static CriterionMatch medium(String justification, Evidence evidence) {
        return new CriterionMatch(justification, ConfidenceLevel.MEDIUM, List.of(evidence));
    }

    /**
     * Creates a LOW confidence match with a single evidence.
     *
     * @param justification the justification
     * @param evidence the evidence
     * @return a new CriterionMatch with LOW confidence
     */
    public static CriterionMatch low(String justification, Evidence evidence) {
        return new CriterionMatch(justification, ConfidenceLevel.LOW, List.of(evidence));
    }
}
