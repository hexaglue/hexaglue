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

import io.hexaglue.arch.AppliedCriterion;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ConflictInfo;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.RemediationHint;
import io.hexaglue.arch.SemanticContext;
import io.hexaglue.syntax.TypeSyntax;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Classifies types as domain concepts (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.).
 *
 * <p>The classifier evaluates all registered criteria against a type and selects
 * the best match using a deterministic tie-break algorithm:</p>
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical order for determinism</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainClassifier classifier = new DomainClassifier(List.of(
 *     new ExplicitAggregateRootCriterion(),
 *     new ExplicitEntityCriterion(),
 *     new RepositoryDominantCriterion()
 * ));
 *
 * ClassificationTrace trace = classifier.classify(typeSyntax, context);
 * System.out.println(trace.classifiedAs()); // AGGREGATE_ROOT
 * System.out.println(trace.explain());      // Full explanation
 * }</pre>
 *
 * <h2>Conflict Detection</h2>
 * <p>When multiple criteria match with different target kinds, the classifier
 * records the conflicts in the trace. This helps users understand why a particular
 * classification was chosen and what alternatives were considered.</p>
 *
 * @since 4.0.0
 */
public final class DomainClassifier {

    private final List<ClassificationCriterion> criteria;

    /**
     * Creates a new DomainClassifier with the given criteria.
     *
     * @param criteria the classification criteria to use
     * @throws NullPointerException if criteria is null
     */
    public DomainClassifier(List<ClassificationCriterion> criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        this.criteria = List.copyOf(criteria);
    }

    /**
     * Classifies a type using the registered criteria.
     *
     * @param type the type to classify
     * @param context the classification context
     * @return a classification trace explaining the decision
     * @throws NullPointerException if type or context is null
     */
    public ClassificationTrace classify(TypeSyntax type, ClassificationContext context) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(context, "context must not be null");

        // Evaluate all criteria
        List<EvaluatedCriterion> evaluations = new ArrayList<>();
        for (ClassificationCriterion criterion : criteria) {
            Optional<CriterionMatch> match = criterion.evaluate(type, context);
            evaluations.add(new EvaluatedCriterion(criterion, match));
        }

        // Filter matched criteria
        List<EvaluatedCriterion> matched =
                evaluations.stream().filter(e -> e.match().isPresent()).toList();

        if (matched.isEmpty()) {
            return createUnclassifiedTrace(evaluations);
        }

        // Sort by priority (desc), confidence (desc), name (asc) for determinism
        List<EvaluatedCriterion> sorted = matched.stream()
                .sorted(Comparator.<EvaluatedCriterion>comparingInt(e -> e.criterion().priority())
                        .reversed()
                        .thenComparing(e -> e.match().orElseThrow().confidence())
                        .thenComparing(e -> e.criterion().name()))
                .toList();

        EvaluatedCriterion winner = sorted.get(0);
        CriterionMatch winnerMatch = winner.match().orElseThrow();

        // Build applied criteria list
        List<AppliedCriterion> appliedCriteria = evaluations.stream()
                .map(e -> toAppliedCriterion(e.criterion(), e.match()))
                .toList();

        // Detect conflicts
        List<ConflictInfo> conflicts = detectConflicts(winner, sorted);

        // Build classification trace
        AppliedCriterion winningApplied = AppliedCriterion.matched(
                winner.criterion().name(),
                winner.criterion().priority(),
                winner.criterion().targetKind(),
                winnerMatch.justification(),
                winnerMatch.evidence());

        return new ClassificationTrace(
                winner.criterion().targetKind(),
                winnerMatch.confidence(),
                winningApplied,
                appliedCriteria,
                conflicts,
                SemanticContext.empty(),
                List.of());
    }

    /**
     * Creates an UNCLASSIFIED trace when no criteria matched.
     */
    private ClassificationTrace createUnclassifiedTrace(List<EvaluatedCriterion> evaluations) {
        List<AppliedCriterion> appliedCriteria = evaluations.stream()
                .map(e -> toAppliedCriterion(e.criterion(), e.match()))
                .toList();

        // Create a "no-match" winning criterion for unclassified
        AppliedCriterion noMatch = new AppliedCriterion(
                "no-matching-criterion", 0, false, ElementKind.UNCLASSIFIED, "No criterion matched", List.of());

        List<RemediationHint> hints = List.of();

        return new ClassificationTrace(
                ElementKind.UNCLASSIFIED,
                ConfidenceLevel.LOW,
                noMatch,
                appliedCriteria,
                List.of(),
                SemanticContext.empty(),
                hints);
    }

    /**
     * Converts an evaluation to an AppliedCriterion.
     */
    private AppliedCriterion toAppliedCriterion(ClassificationCriterion criterion, Optional<CriterionMatch> match) {
        if (match.isPresent()) {
            CriterionMatch m = match.get();
            return AppliedCriterion.matched(
                    criterion.name(), criterion.priority(), criterion.targetKind(), m.justification(), m.evidence());
        }
        return AppliedCriterion.unmatched(criterion.name(), criterion.priority(), criterion.targetKind());
    }

    /**
     * Detects conflicts between the winner and other matched criteria.
     */
    private List<ConflictInfo> detectConflicts(EvaluatedCriterion winner, List<EvaluatedCriterion> sorted) {
        ElementKind winnerKind = winner.criterion().targetKind();
        List<ConflictInfo> conflicts = new ArrayList<>();

        for (EvaluatedCriterion evaluation : sorted) {
            if (evaluation == winner) {
                continue;
            }
            ElementKind kind = evaluation.criterion().targetKind();
            if (kind != winnerKind) {
                CriterionMatch match = evaluation.match().orElseThrow();
                conflicts.add(new ConflictInfo(kind, match.justification(), match.confidence()));
            }
        }

        return List.copyOf(conflicts);
    }

    /**
     * Returns the list of criteria used by this classifier.
     *
     * @return the criteria list
     */
    public List<ClassificationCriterion> criteria() {
        return criteria;
    }

    /**
     * Internal record to hold criterion evaluation result.
     */
    private record EvaluatedCriterion(ClassificationCriterion criterion, Optional<CriterionMatch> match) {}
}
