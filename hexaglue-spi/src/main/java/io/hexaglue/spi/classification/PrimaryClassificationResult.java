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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.classification.CertaintyLevel;
import io.hexaglue.arch.model.classification.ClassificationEvidence;
import io.hexaglue.arch.model.classification.ClassificationStrategy;
import java.util.List;
import java.util.Objects;

/**
 * Result of primary (deterministic) classification.
 *
 * <p>Primary classification results are produced by the built-in HexaGlue
 * classification engine using deterministic rules and structural patterns.
 * These results serve as input to secondary classifiers, which can choose
 * to accept, override, or refine them.
 *
 * <h2>Classification Quality</h2>
 * <p>The quality of a primary classification can be assessed through:
 * <ul>
 *   <li>{@link #certainty()} - How confident the classifier is</li>
 *   <li>{@link #strategy()} - Which strategy was used</li>
 *   <li>{@link #evidences()} - What signals supported the decision</li>
 * </ul>
 *
 * <h2>Usage in Secondary Classifiers</h2>
 * <pre>{@code
 * public SecondaryClassificationResult classify(
 *         TypeInfo type,
 *         ClassificationContext context,
 *         Optional<PrimaryClassificationResult> primaryResult) {
 *
 *     // Trust high-certainty primary results
 *     if (primaryResult.isPresent() &&
 *         primaryResult.get().certainty().isReliable()) {
 *         return null; // Use primary
 *     }
 *
 *     // Override uncertain primary results with custom logic
 *     // ...
 * }
 * }</pre>
 *
 * @param typeName  the fully qualified type name
 * @param kind      the classified element kind (may be null if unclassified)
 * @param certainty the certainty level of the classification
 * @param strategy  the strategy used to produce this classification
 * @param reasoning human-readable explanation of the classification
 * @param evidences list of evidence supporting this classification
 * @since 3.0.0
 * @since 4.0.0 Changed kind type from ElementKind to ElementKind
 */
public record PrimaryClassificationResult(
        String typeName,
        ElementKind kind,
        CertaintyLevel certainty,
        ClassificationStrategy strategy,
        String reasoning,
        List<ClassificationEvidence> evidences) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any non-nullable parameter is null
     */
    public PrimaryClassificationResult {
        Objects.requireNonNull(typeName, "typeName required");
        // kind can be null for unclassified types
        Objects.requireNonNull(certainty, "certainty required");
        Objects.requireNonNull(strategy, "strategy required");
        Objects.requireNonNull(reasoning, "reasoning required");
        Objects.requireNonNull(evidences, "evidences required");
    }

    /**
     * Returns true if this result indicates the type was successfully classified.
     *
     * @return true if kind is not null and certainty is not NONE
     */
    public boolean isClassified() {
        return kind != null && certainty != CertaintyLevel.NONE;
    }

    /**
     * Returns true if this classification is reliable enough for code generation.
     *
     * <p>Delegates to {@link CertaintyLevel#isReliable()}.
     *
     * @return true if certainty is EXPLICIT or CERTAIN_BY_STRUCTURE
     */
    public boolean isReliable() {
        return certainty.isReliable();
    }

    /**
     * Returns true if this classification should be reviewed by a developer.
     *
     * <p>Delegates to {@link CertaintyLevel#needsReview()}.
     *
     * @return true if certainty is UNCERTAIN or NONE
     */
    public boolean needsReview() {
        return certainty.needsReview();
    }

    /**
     * Returns the total weight of all evidence.
     *
     * @return sum of all evidence weights
     */
    public int totalEvidenceWeight() {
        return evidences.stream().mapToInt(ClassificationEvidence::weight).sum();
    }

    /**
     * Creates an unclassified result.
     *
     * @param typeName the type name
     * @param reasoning the reason for non-classification
     * @return unclassified result
     */
    public static PrimaryClassificationResult unclassified(String typeName, String reasoning) {
        return new PrimaryClassificationResult(
                typeName, null, CertaintyLevel.NONE, ClassificationStrategy.UNCLASSIFIED, reasoning, List.of());
    }
}
