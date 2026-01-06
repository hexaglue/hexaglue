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

package io.hexaglue.core.classification;

import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import java.util.Objects;

/**
 * Detailed trace of how a classification decision was made.
 *
 * <p>ReasonTrace provides debugging information about:
 * <ul>
 *   <li>All criteria that were evaluated (matched or not)</li>
 *   <li>Graph edges that contributed to the decision</li>
 *   <li>The anchor classification of the type (if applicable)</li>
 * </ul>
 *
 * <p>This is useful for:
 * <ul>
 *   <li>Understanding why a type was classified a certain way</li>
 *   <li>Debugging unexpected classifications</li>
 *   <li>Generating detailed classification reports</li>
 * </ul>
 *
 * @param evaluatedCriteria all criteria that were evaluated, in order
 * @param contributingEdges graph edges that contributed to the classification
 * @param anchorKind the anchor kind of the type, if detected (e.g., "DOMAIN_ANCHOR")
 * @param coreAppClassRole the CoreAppClass role, if applicable (e.g., "PIVOT", "INBOUND_ONLY")
 */
public record ReasonTrace(
        List<EvaluatedCriteria> evaluatedCriteria,
        List<ContributingEdge> contributingEdges,
        String anchorKind,
        String coreAppClassRole) {

    public ReasonTrace {
        evaluatedCriteria = evaluatedCriteria == null ? List.of() : List.copyOf(evaluatedCriteria);
        contributingEdges = contributingEdges == null ? List.of() : List.copyOf(contributingEdges);
    }

    /**
     * Creates an empty reason trace.
     */
    public static ReasonTrace empty() {
        return new ReasonTrace(List.of(), List.of(), null, null);
    }

    /**
     * Creates a builder for constructing a ReasonTrace.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if this trace has any evaluated criteria.
     */
    public boolean hasEvaluatedCriteria() {
        return !evaluatedCriteria.isEmpty();
    }

    /**
     * Returns true if this trace has any contributing edges.
     */
    public boolean hasContributingEdges() {
        return !contributingEdges.isEmpty();
    }

    /**
     * Returns all criteria that matched.
     */
    public List<EvaluatedCriteria> matchedCriteria() {
        return evaluatedCriteria.stream().filter(EvaluatedCriteria::matched).toList();
    }

    /**
     * Returns all criteria that did not match.
     */
    public List<EvaluatedCriteria> unmatchedCriteria() {
        return evaluatedCriteria.stream().filter(c -> !c.matched()).toList();
    }

    /**
     * Record of a criteria evaluation.
     *
     * @param criteriaName the name of the criteria
     * @param matched whether the criteria matched
     * @param priority the priority of the criteria
     * @param targetKind the kind this criteria targets
     * @param confidence the confidence level if matched
     * @param reason the reason for match/no-match
     */
    public record EvaluatedCriteria(
            String criteriaName,
            boolean matched,
            int priority,
            String targetKind,
            ConfidenceLevel confidence,
            String reason) {

        public EvaluatedCriteria {
            Objects.requireNonNull(criteriaName, "criteriaName cannot be null");
        }

        /**
         * Creates a matched criteria record.
         */
        public static EvaluatedCriteria matched(
                String name, int priority, String targetKind, ConfidenceLevel confidence, String reason) {
            return new EvaluatedCriteria(name, true, priority, targetKind, confidence, reason);
        }

        /**
         * Creates an unmatched criteria record.
         */
        public static EvaluatedCriteria unmatched(String name, int priority, String targetKind, String reason) {
            return new EvaluatedCriteria(name, false, priority, targetKind, null, reason);
        }
    }

    /**
     * Record of a graph edge that contributed to the classification.
     *
     * @param from the source node id
     * @param to the target node id
     * @param edgeKind the kind of edge
     * @param contribution how this edge contributed to the decision
     */
    public record ContributingEdge(NodeId from, NodeId to, EdgeKind edgeKind, String contribution) {

        public ContributingEdge {
            Objects.requireNonNull(from, "from cannot be null");
            Objects.requireNonNull(to, "to cannot be null");
            Objects.requireNonNull(edgeKind, "edgeKind cannot be null");
        }
    }

    /**
     * Builder for ReasonTrace.
     */
    public static final class Builder {

        private final java.util.ArrayList<EvaluatedCriteria> evaluatedCriteria = new java.util.ArrayList<>();
        private final java.util.ArrayList<ContributingEdge> contributingEdges = new java.util.ArrayList<>();
        private String anchorKind;
        private String coreAppClassRole;

        private Builder() {}

        /**
         * Adds an evaluated criteria.
         */
        public Builder addEvaluatedCriteria(EvaluatedCriteria criteria) {
            evaluatedCriteria.add(Objects.requireNonNull(criteria));
            return this;
        }

        /**
         * Adds a contributing edge.
         */
        public Builder addContributingEdge(ContributingEdge edge) {
            contributingEdges.add(Objects.requireNonNull(edge));
            return this;
        }

        /**
         * Adds a contributing edge.
         */
        public Builder addContributingEdge(NodeId from, NodeId to, EdgeKind kind, String contribution) {
            return addContributingEdge(new ContributingEdge(from, to, kind, contribution));
        }

        /**
         * Sets the anchor kind.
         */
        public Builder anchorKind(String anchorKind) {
            this.anchorKind = anchorKind;
            return this;
        }

        /**
         * Sets the CoreAppClass role.
         */
        public Builder coreAppClassRole(String role) {
            this.coreAppClassRole = role;
            return this;
        }

        /**
         * Builds the ReasonTrace.
         */
        public ReasonTrace build() {
            return new ReasonTrace(evaluatedCriteria, contributingEdges, anchorKind, coreAppClassRole);
        }
    }
}
