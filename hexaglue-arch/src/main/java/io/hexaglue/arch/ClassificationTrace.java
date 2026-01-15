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
 * Explainable trace of a classification decision.
 *
 * <p>Answers the question: "Why was this element classified as X?"</p>
 *
 * <p>This record provides full transparency into the classification process,
 * including the winning criterion, all evaluated criteria, any conflicts,
 * and suggested remediation actions.</p>
 *
 * <h2>Example Output</h2>
 * <pre>
 * Classification: AGGREGATE_ROOT (HIGH)
 *
 * Why: Type is used as primary managed type in repository OrderRepository
 *      and has identity field 'id' of type OrderId
 *
 * Conflicts considered:
 *   - ENTITY: Also has identity, but repository usage indicates aggregate root
 *
 * All evaluated criteria:
 *   [X] explicit-aggregate-root (priority=100) -> AGGREGATE_ROOT
 *   [X] repository-primary-type (priority=85) -> AGGREGATE_ROOT
 *   [X] has-identity-field (priority=80) -> ENTITY
 *   [ ] jmolecules-entity (priority=100)
 *
 * To make classification explicit:
 *   → Add @AggregateRoot annotation (HIGH confidence)
 * </pre>
 *
 * @param classifiedAs the resulting element kind
 * @param confidence the confidence level of the classification
 * @param winningCriterion the criterion that determined the classification
 * @param evaluatedCriteria all criteria that were evaluated
 * @param conflicts any conflicting classifications that were considered
 * @param semanticContext the semantic context used during classification
 * @param remediationHints suggested actions to make classification explicit
 * @since 4.0.0
 */
public record ClassificationTrace(
        ElementKind classifiedAs,
        ConfidenceLevel confidence,
        AppliedCriterion winningCriterion,
        List<AppliedCriterion> evaluatedCriteria,
        List<ConflictInfo> conflicts,
        SemanticContext semanticContext,
        List<RemediationHint> remediationHints) {

    /**
     * Creates a new ClassificationTrace instance.
     *
     * @param classifiedAs the classified kind, must not be null
     * @param confidence the confidence level, must not be null
     * @param winningCriterion the winning criterion, must not be null
     * @param evaluatedCriteria the evaluated criteria, must not be null
     * @param conflicts the conflicts, must not be null
     * @param semanticContext the semantic context, must not be null
     * @param remediationHints the remediation hints, must not be null
     * @throws NullPointerException if any field is null
     */
    public ClassificationTrace {
        Objects.requireNonNull(classifiedAs, "classifiedAs must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(winningCriterion, "winningCriterion must not be null");
        Objects.requireNonNull(evaluatedCriteria, "evaluatedCriteria must not be null");
        Objects.requireNonNull(conflicts, "conflicts must not be null");
        Objects.requireNonNull(semanticContext, "semanticContext must not be null");
        Objects.requireNonNull(remediationHints, "remediationHints must not be null");
        evaluatedCriteria = List.copyOf(evaluatedCriteria);
        conflicts = List.copyOf(conflicts);
        remediationHints = List.copyOf(remediationHints);
    }

    /**
     * Returns a human-readable explanation of the classification.
     *
     * <p>Includes the classification result, winning criterion, conflicts,
     * evaluated criteria, and remediation hints.</p>
     *
     * @return a multi-line explanation string
     */
    public String explain() {
        StringBuilder sb = new StringBuilder();

        sb.append("Classification: ")
                .append(classifiedAs)
                .append(" (")
                .append(confidence)
                .append(")\n");

        sb.append("\nWhy: ").append(winningCriterion.explanation()).append("\n");

        if (!conflicts.isEmpty()) {
            sb.append("\nConflicts considered:\n");
            for (ConflictInfo conflict : conflicts) {
                sb.append("  - ")
                        .append(conflict.alternativeKind())
                        .append(": ")
                        .append(conflict.reason())
                        .append("\n");
            }
        }

        sb.append("\nAll evaluated criteria:\n");
        for (AppliedCriterion criterion : evaluatedCriteria) {
            sb.append("  ")
                    .append(criterion.matched() ? "[X]" : "[ ]")
                    .append(" ")
                    .append(criterion.name())
                    .append(" (priority=")
                    .append(criterion.priority())
                    .append(")");
            if (criterion.matched()) {
                sb.append(" -> ").append(criterion.suggestedKind());
            }
            sb.append("\n");
        }

        if (!remediationHints.isEmpty()) {
            sb.append("\nTo make classification explicit:\n");
            for (RemediationHint hint : remediationHints) {
                sb.append("  → ")
                        .append(hint.action())
                        .append(" (")
                        .append(hint.impact().description())
                        .append(")\n");
            }
        }

        return sb.toString();
    }

    /**
     * Returns a brief one-line explanation.
     *
     * @return a single-line explanation
     */
    public String explainBrief() {
        return classifiedAs + " (" + confidence + "): " + winningCriterion.explanation();
    }

    /**
     * Returns whether this classification needs clarification.
     *
     * <p>A classification needs clarification if:</p>
     * <ul>
     *   <li>Confidence is LOW</li>
     *   <li>The element is UNCLASSIFIED</li>
     *   <li>There are conflicting classifications</li>
     * </ul>
     *
     * @return true if clarification is recommended
     */
    public boolean needsClarification() {
        return confidence == ConfidenceLevel.LOW || classifiedAs == ElementKind.UNCLASSIFIED || !conflicts.isEmpty();
    }

    /**
     * Creates a high-confidence classification trace.
     *
     * <p>Convenience factory for explicit, unambiguous classifications.</p>
     *
     * @param kind the element kind
     * @param criterionName the name of the winning criterion
     * @param explanation the explanation
     * @return a new ClassificationTrace with HIGH confidence
     */
    public static ClassificationTrace highConfidence(ElementKind kind, String criterionName, String explanation) {
        AppliedCriterion winning = AppliedCriterion.matched(criterionName, 100, kind, explanation, List.of());
        return new ClassificationTrace(
                kind, ConfidenceLevel.HIGH, winning, List.of(winning), List.of(), SemanticContext.empty(), List.of());
    }

    /**
     * Creates an UNCLASSIFIED trace.
     *
     * <p>Used when no criterion matched with sufficient confidence.</p>
     *
     * @param reason explanation of why classification failed
     * @param hints remediation hints for the user
     * @return a new ClassificationTrace for UNCLASSIFIED
     */
    public static ClassificationTrace unclassified(String reason, List<RemediationHint> hints) {
        AppliedCriterion noCriterion = AppliedCriterion.unmatched("no-matching-criterion", 0, ElementKind.UNCLASSIFIED);
        return new ClassificationTrace(
                ElementKind.UNCLASSIFIED,
                ConfidenceLevel.LOW,
                new AppliedCriterion("no-match", 0, false, ElementKind.UNCLASSIFIED, reason, List.of()),
                List.of(noCriterion),
                List.of(),
                SemanticContext.empty(),
                hints);
    }
}
