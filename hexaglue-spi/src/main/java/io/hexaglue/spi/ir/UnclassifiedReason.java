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

package io.hexaglue.spi.ir;

import java.util.List;

/**
 * Provides detailed information about why a type could not be classified.
 *
 * <p>This record is populated when a type is marked as {@link DomainKind#UNCLASSIFIED}
 * and provides:
 * <ul>
 *   <li>The criteria that were attempted but did not match</li>
 *   <li>Suggested actions for the user to resolve the classification</li>
 * </ul>
 *
 * @param attemptedCriteria the classification criteria that were evaluated
 * @param suggestedActions recommended actions to resolve the classification
 * @since 3.0.0
 */
public record UnclassifiedReason(
        List<String> attemptedCriteria,
        List<String> suggestedActions) {

    /**
     * Creates an UnclassifiedReason with the given criteria and default suggestions.
     *
     * @param attemptedCriteria the criteria that were evaluated
     * @return a new UnclassifiedReason with default suggestions
     */
    public static UnclassifiedReason withDefaultSuggestions(List<String> attemptedCriteria) {
        return new UnclassifiedReason(
                attemptedCriteria,
                List.of(
                        "Add explicit jMolecules annotation (@AggregateRoot, @Entity, @ValueObject, etc.)",
                        "Configure classification hints in hexaglue.yaml"));
    }

    /**
     * Creates an UnclassifiedReason indicating no criteria matched.
     *
     * @return a new UnclassifiedReason for the no-match case
     */
    public static UnclassifiedReason noCriteriaMatched() {
        return new UnclassifiedReason(
                List.of(),
                List.of(
                        "Add explicit jMolecules annotation to classify this type",
                        "The type may be outside the domain (utility, infrastructure, etc.)"));
    }

    /**
     * Creates an UnclassifiedReason indicating conflicting classifications.
     *
     * @param conflictingKinds the kinds that conflicted
     * @return a new UnclassifiedReason for the conflict case
     */
    public static UnclassifiedReason conflictingClassifications(List<String> conflictingKinds) {
        return new UnclassifiedReason(
                conflictingKinds,
                List.of(
                        "Add explicit jMolecules annotation to resolve ambiguity",
                        "Review the type structure - it may need refactoring"));
    }
}
