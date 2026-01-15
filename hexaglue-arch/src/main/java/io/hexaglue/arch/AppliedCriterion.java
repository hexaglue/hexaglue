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

import java.util.List;
import java.util.Objects;

/**
 * A criterion that was applied during classification.
 *
 * <p>Records whether a classification criterion matched, its priority,
 * and what classification it would suggest.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * AppliedCriterion criterion = new AppliedCriterion(
 *     "explicit-aggregate-root",
 *     100,
 *     true,
 *     ElementKind.AGGREGATE_ROOT,
 *     "Type has @AggregateRoot annotation",
 *     List.of(evidence)
 * );
 * }</pre>
 *
 * @param name unique name of the criterion
 * @param priority priority value (higher = more important)
 * @param matched whether the criterion matched
 * @param suggestedKind the element kind this criterion suggests
 * @param explanation human-readable explanation of the match
 * @param evidence list of evidence supporting the match
 * @since 4.0.0
 */
public record AppliedCriterion(
        String name,
        int priority,
        boolean matched,
        ElementKind suggestedKind,
        String explanation,
        List<Evidence> evidence) {

    /**
     * Creates a new AppliedCriterion instance.
     *
     * @param name the criterion name, must not be null or blank
     * @param priority the priority value
     * @param matched whether the criterion matched
     * @param suggestedKind the suggested element kind, must not be null
     * @param explanation the explanation, must not be null
     * @param evidence the list of evidence, must not be null
     * @throws NullPointerException if any required field is null
     * @throws IllegalArgumentException if name is blank
     */
    public AppliedCriterion {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(suggestedKind, "suggestedKind must not be null");
        Objects.requireNonNull(explanation, "explanation must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        evidence = List.copyOf(evidence);
    }

    /**
     * Creates a matched criterion.
     *
     * @param name the criterion name
     * @param priority the priority
     * @param suggestedKind the suggested kind
     * @param explanation the explanation
     * @param evidence the evidence list
     * @return a new matched AppliedCriterion
     */
    public static AppliedCriterion matched(
            String name, int priority, ElementKind suggestedKind, String explanation, List<Evidence> evidence) {
        return new AppliedCriterion(name, priority, true, suggestedKind, explanation, evidence);
    }

    /**
     * Creates an unmatched criterion.
     *
     * @param name the criterion name
     * @param priority the priority
     * @param suggestedKind the kind this criterion would suggest if matched
     * @return a new unmatched AppliedCriterion
     */
    public static AppliedCriterion unmatched(String name, int priority, ElementKind suggestedKind) {
        return new AppliedCriterion(name, priority, false, suggestedKind, "", List.of());
    }
}
