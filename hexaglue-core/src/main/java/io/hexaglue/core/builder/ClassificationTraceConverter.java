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

package io.hexaglue.core.builder;

import io.hexaglue.arch.AppliedCriterion;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ConflictInfo;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.SemanticContext;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.Conflict;
import java.util.List;
import java.util.Objects;

/**
 * Converts classification results from hexaglue-core to ClassificationTrace for hexaglue-arch.
 *
 * <p>This converter bridges the classification system (hexaglue-core) with the architectural
 * model (hexaglue-arch). It transforms {@link ClassificationResult} into {@link ClassificationTrace},
 * mapping confidence levels, evidence, and conflicts.</p>
 *
 * <h2>Mapping Rules</h2>
 * <ul>
 *   <li>Kind string → {@link ElementKind} enum (e.g., "AGGREGATE_ROOT" → AGGREGATE_ROOT)</li>
 *   <li>Core ConfidenceLevel → Arch ConfidenceLevel (EXPLICIT/HIGH → HIGH, MEDIUM → MEDIUM, LOW → LOW)</li>
 *   <li>Core Evidence → Arch Evidence with type and description</li>
 *   <li>Core Conflict → Arch ConflictInfo</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ClassificationTraceConverter converter = new ClassificationTraceConverter();
 * ClassificationTrace trace = converter.convert(classificationResult);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class ClassificationTraceConverter {

    /**
     * Creates a new ClassificationTraceConverter.
     */
    public ClassificationTraceConverter() {
        // Stateless converter
    }

    /**
     * Converts a classification result to a classification trace.
     *
     * @param classificationResult the classification result to convert
     * @return the converted classification trace
     * @throws NullPointerException if classificationResult is null
     */
    public ClassificationTrace convert(ClassificationResult classificationResult) {
        Objects.requireNonNull(classificationResult, "classificationResult must not be null");

        ElementKind kind = mapKind(classificationResult.kind());
        ConfidenceLevel confidence = mapConfidence(classificationResult.confidence());

        AppliedCriterion winningCriterion = createWinningCriterion(classificationResult, kind);

        List<AppliedCriterion> evaluatedCriteria =
                createEvaluatedCriteria(classificationResult, kind, winningCriterion);

        List<ConflictInfo> conflicts = mapConflicts(classificationResult.conflicts());

        return new ClassificationTrace(
                kind, confidence, winningCriterion, evaluatedCriteria, conflicts, SemanticContext.empty(), List.of());
    }

    private ElementKind mapKind(String kindString) {
        if (kindString == null || kindString.isEmpty()) {
            return ElementKind.UNCLASSIFIED;
        }

        try {
            return ElementKind.valueOf(kindString);
        } catch (IllegalArgumentException e) {
            return ElementKind.UNCLASSIFIED;
        }
    }

    private ConfidenceLevel mapConfidence(io.hexaglue.core.classification.ConfidenceLevel coreConfidence) {
        if (coreConfidence == null) {
            return ConfidenceLevel.LOW;
        }

        return switch (coreConfidence) {
            case EXPLICIT, HIGH -> ConfidenceLevel.HIGH;
            case MEDIUM -> ConfidenceLevel.MEDIUM;
            case LOW -> ConfidenceLevel.LOW;
        };
    }

    private AppliedCriterion createWinningCriterion(ClassificationResult result, ElementKind kind) {
        String criteriaName = result.matchedCriteria();
        int priority = result.matchedPriority();
        String explanation = result.justification();

        if (criteriaName == null || criteriaName.isEmpty()) {
            return AppliedCriterion.unmatched("no-matching-criterion", 0, kind);
        }

        return AppliedCriterion.matched(
                criteriaName, priority, kind, explanation != null ? explanation : "", List.of());
    }

    private List<AppliedCriterion> createEvaluatedCriteria(
            ClassificationResult result, ElementKind kind, AppliedCriterion winningCriterion) {
        // If there's evidence, convert it to additional evaluated criteria
        if (!result.evidence().isEmpty()) {
            List<AppliedCriterion> fromEvidence = result.evidence().stream()
                    .map(e -> createCriterionFromEvidence(e, kind))
                    .toList();

            // Include winning criterion if it's not already in the list
            if (fromEvidence.stream().noneMatch(c -> c.name().equals(winningCriterion.name()))) {
                return List.of(winningCriterion);
            }
            return fromEvidence;
        }

        return List.of(winningCriterion);
    }

    private AppliedCriterion createCriterionFromEvidence(
            io.hexaglue.core.classification.Evidence coreEvidence, ElementKind kind) {
        String name = coreEvidence.type().name().toLowerCase() + "-evidence";
        return AppliedCriterion.matched(name, 50, kind, coreEvidence.description(), List.of());
    }

    private List<ConflictInfo> mapConflicts(List<Conflict> coreConflicts) {
        return coreConflicts.stream().map(this::mapConflict).toList();
    }

    private ConflictInfo mapConflict(Conflict coreConflict) {
        ElementKind alternativeKind = mapKind(coreConflict.competingKind());
        ConfidenceLevel alternativeConfidence = mapConfidence(coreConflict.competingConfidence());
        String reason = coreConflict.rationale();
        return new ConflictInfo(alternativeKind, reason, alternativeConfidence);
    }
}
