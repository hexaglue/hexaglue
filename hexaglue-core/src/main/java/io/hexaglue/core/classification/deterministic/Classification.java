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

package io.hexaglue.core.classification.deterministic;

import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.classification.ClassificationStrategy;
import io.hexaglue.spi.ir.DomainKind;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A classification decision for a domain type.
 *
 * <p>This record captures the complete classification result including:
 * <ul>
 *   <li>The domain kind assigned (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.)</li>
 *   <li>The certainty level of the classification</li>
 *   <li>The strategy used to determine the classification</li>
 *   <li>Human-readable reasoning</li>
 *   <li>Supporting evidence (for transparency and debugging)</li>
 * </ul>
 *
 * @param typeName  the fully qualified type name
 * @param kind      the assigned domain kind
 * @param certainty the certainty level
 * @param strategy  the classification strategy used
 * @param reasoning human-readable explanation
 * @param evidences list of supporting evidence
 * @since 3.0.0
 */
public record Classification(
        String typeName,
        DomainKind kind,
        CertaintyLevel certainty,
        ClassificationStrategy strategy,
        String reasoning,
        List<ClassificationEvidence> evidences) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public Classification {
        Objects.requireNonNull(typeName, "typeName required");
        Objects.requireNonNull(kind, "kind required");
        Objects.requireNonNull(certainty, "certainty required");
        Objects.requireNonNull(strategy, "strategy required");
        Objects.requireNonNull(reasoning, "reasoning required");
        evidences = evidences != null ? Collections.unmodifiableList(List.copyOf(evidences)) : List.of();
    }

    /**
     * Creates a classification with explicit certainty (annotation-based).
     *
     * @param typeName  the type name
     * @param kind      the domain kind
     * @param reasoning the reasoning
     * @return new classification with EXPLICIT certainty and ANNOTATION strategy
     */
    public static Classification explicit(String typeName, DomainKind kind, String reasoning) {
        return new Classification(
                typeName,
                kind,
                CertaintyLevel.EXPLICIT,
                ClassificationStrategy.ANNOTATION,
                reasoning,
                List.of(ClassificationEvidence.positive(
                        "EXPLICIT_ANNOTATION", 100, "Type has explicit classification annotation")));
    }

    /**
     * Creates a classification from a repository pattern.
     *
     * @param typeName  the type name
     * @param reasoning the reasoning
     * @param evidences the evidence
     * @return new classification as AGGREGATE_ROOT with REPOSITORY strategy
     */
    public static Classification fromRepository(
            String typeName, String reasoning, List<ClassificationEvidence> evidences) {
        return new Classification(
                typeName,
                DomainKind.AGGREGATE_ROOT,
                CertaintyLevel.CERTAIN_BY_STRUCTURE,
                ClassificationStrategy.REPOSITORY,
                reasoning,
                evidences);
    }

    /**
     * Creates a classification from a record pattern.
     *
     * @param typeName  the type name
     * @param reasoning the reasoning
     * @param evidences the evidence
     * @return new classification as VALUE_OBJECT with RECORD strategy
     */
    public static Classification fromRecord(String typeName, String reasoning, List<ClassificationEvidence> evidences) {
        return new Classification(
                typeName,
                DomainKind.VALUE_OBJECT,
                CertaintyLevel.CERTAIN_BY_STRUCTURE,
                ClassificationStrategy.RECORD,
                reasoning,
                evidences);
    }

    /**
     * Creates a classification from composition analysis.
     *
     * @param typeName  the type name
     * @param kind      the domain kind
     * @param reasoning the reasoning
     * @param evidences the evidence
     * @return new classification with COMPOSITION strategy
     */
    public static Classification fromComposition(
            String typeName, DomainKind kind, String reasoning, List<ClassificationEvidence> evidences) {
        return new Classification(
                typeName, kind, CertaintyLevel.INFERRED, ClassificationStrategy.COMPOSITION, reasoning, evidences);
    }

    /**
     * Creates an unclassified result.
     *
     * @param typeName  the type name
     * @param reasoning the reasoning
     * @return new classification with no kind assigned
     */
    public static Classification unclassified(String typeName, String reasoning) {
        return new Classification(
                typeName, null, CertaintyLevel.NONE, ClassificationStrategy.UNCLASSIFIED, reasoning, List.of());
    }

    /**
     * Returns true if this classification is reliable enough for code generation.
     *
     * @return true if certainty is EXPLICIT or CERTAIN_BY_STRUCTURE
     */
    public boolean isReliable() {
        return certainty.isReliable();
    }

    /**
     * Returns true if this classification needs manual review.
     *
     * @return true if certainty is UNCERTAIN or NONE
     */
    public boolean needsReview() {
        return certainty.needsReview();
    }

    /**
     * Returns true if this type was classified (not unclassified).
     *
     * @return true if kind is not null
     */
    public boolean isClassified() {
        return kind != null;
    }

    /**
     * Returns a summary string for reporting.
     *
     * @return formatted summary
     */
    public String toSummaryString() {
        String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        return String.format("%s: %s [%s, %s]", simpleName, kind, certainty, strategy);
    }

    @Override
    public String toString() {
        return toSummaryString();
    }
}
