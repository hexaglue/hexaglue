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

package io.hexaglue.core.classification.engine;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic classification engine that evaluates criteria and makes decisions.
 *
 * <p>This engine provides a reusable foundation for both domain and port
 * classification. It:
 * <ol>
 *   <li>Evaluates all criteria against a type node</li>
 *   <li>Collects contributions from matching criteria</li>
 *   <li>Delegates to a {@link DecisionPolicy} to select the winner</li>
 * </ol>
 *
 * <p>The engine is parameterized by:
 * <ul>
 *   <li>{@code K} - the kind enum type (e.g., ElementKind, PortKind)</li>
 *   <li>{@code C} - the criteria type (must extend ClassificationCriteria)</li>
 * </ul>
 *
 * <p>Priorities are taken directly from each criteria's {@code priority()} method.
 *
 * @param <K> the kind enum type (must extend Enum)
 * @param <C> the criteria type
 */
public final class CriteriaEngine<K extends Enum<K>, C extends ClassificationCriteria<K>> {

    private final List<C> criteria;
    private final DecisionPolicy<K> decisionPolicy;
    private final CompatibilityPolicy<K> compatibilityPolicy;
    private final ContributionBuilder<K, C> contributionBuilder;

    /**
     * Creates a new criteria engine.
     *
     * @param criteria the list of criteria to evaluate
     * @param decisionPolicy the policy for making decisions
     * @param compatibilityPolicy the policy for determining kind compatibility
     * @param contributionBuilder function to build contributions from match results
     */
    public CriteriaEngine(
            List<C> criteria,
            DecisionPolicy<K> decisionPolicy,
            CompatibilityPolicy<K> compatibilityPolicy,
            ContributionBuilder<K, C> contributionBuilder) {
        this.criteria = List.copyOf(Objects.requireNonNull(criteria, "criteria cannot be null"));
        this.decisionPolicy = Objects.requireNonNull(decisionPolicy, "decisionPolicy cannot be null");
        this.compatibilityPolicy = Objects.requireNonNull(compatibilityPolicy, "compatibilityPolicy cannot be null");
        this.contributionBuilder = Objects.requireNonNull(contributionBuilder, "contributionBuilder cannot be null");
    }

    /**
     * Creates a new criteria engine with the default decision policy.
     *
     * @param criteria the list of criteria to evaluate
     * @param compatibilityPolicy the policy for determining kind compatibility
     * @param contributionBuilder function to build contributions from match results
     */
    public CriteriaEngine(
            List<C> criteria,
            CompatibilityPolicy<K> compatibilityPolicy,
            ContributionBuilder<K, C> contributionBuilder) {
        this(criteria, new DefaultDecisionPolicy<>(), compatibilityPolicy, contributionBuilder);
    }

    /**
     * Evaluates all criteria against a type node and returns contributions.
     *
     * @param node the type node to classify
     * @param query the graph query for accessing graph data
     * @return list of contributions from matching criteria
     */
    public List<Contribution<K>> evaluate(TypeNode node, GraphQuery query) {
        List<Contribution<K>> contributions = new ArrayList<>();

        for (C criterion : criteria) {
            MatchResult result = criterion.evaluate(node, query);
            if (result.matched()) {
                Contribution<K> contribution = contributionBuilder.build(criterion, result, criterion.priority());
                contributions.add(contribution);
            }
        }

        return contributions;
    }

    /**
     * Classifies a type node and returns the decision.
     *
     * @param node the type node to classify
     * @param query the graph query for accessing graph data
     * @return the classification decision
     */
    public DecisionPolicy.Decision<K> classify(TypeNode node, GraphQuery query) {
        List<Contribution<K>> contributions = evaluate(node, query);
        return decisionPolicy.decide(contributions, compatibilityPolicy);
    }

    /**
     * Returns the list of criteria in this engine.
     */
    public List<C> criteria() {
        return criteria;
    }

    /**
     * Functional interface for building contributions from match results.
     *
     * @param <K> the kind type
     * @param <C> the criteria type
     */
    @FunctionalInterface
    public interface ContributionBuilder<K extends Enum<K>, C extends ClassificationCriteria<K>> {

        /**
         * Builds a contribution from a matching criteria.
         *
         * @param criteria the criteria that matched
         * @param result the match result
         * @param effectivePriority the priority to use (may be overridden by profile)
         * @return the contribution
         */
        Contribution<K> build(C criteria, MatchResult result, int effectivePriority);
    }
}
