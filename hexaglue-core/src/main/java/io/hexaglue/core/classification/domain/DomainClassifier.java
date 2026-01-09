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

package io.hexaglue.core.classification.domain;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.classification.ConflictSeverity;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.criteria.CollectionElementEntityCriteria;
import io.hexaglue.core.classification.domain.criteria.EmbeddedValueObjectCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitAggregateRootCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitDomainEventCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitEntityCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitExternalizedEventCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitIdentifierCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitValueObjectCriteria;
import io.hexaglue.core.classification.domain.criteria.HasIdentityCriteria;
import io.hexaglue.core.classification.domain.criteria.HasPortDependenciesCriteria;
import io.hexaglue.core.classification.domain.criteria.ImmutableNoIdCriteria;
import io.hexaglue.core.classification.domain.criteria.ImplementsJMoleculesInterfaceCriteria;
import io.hexaglue.core.classification.domain.criteria.InheritedClassificationCriteria;
import io.hexaglue.core.classification.domain.criteria.NamingDomainEventCriteria;
import io.hexaglue.core.classification.domain.criteria.RecordSingleIdCriteria;
import io.hexaglue.core.classification.domain.criteria.RepositoryDominantCriteria;
import io.hexaglue.core.classification.domain.criteria.StatelessNoDependenciesCriteria;
import io.hexaglue.core.classification.domain.criteria.UnreferencedInPortsCriteria;
import io.hexaglue.core.classification.engine.CompatibilityPolicy;
import io.hexaglue.core.classification.engine.Contribution;
import io.hexaglue.core.classification.engine.CriteriaEngine;
import io.hexaglue.core.classification.engine.CriteriaProfile;
import io.hexaglue.core.classification.engine.DecisionPolicy;
import io.hexaglue.core.classification.engine.DefaultDecisionPolicy;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Classifies types as domain concepts (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.).
 *
 * <p>The classifier evaluates all registered criteria against a type and selects
 * the best match using a deterministic tie-break algorithm:
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical order for determinism</li>
 * </ol>
 *
 * <p>Conflicts are detected when multiple criteria match with different target kinds.
 * If the conflicts are incompatible (e.g., ENTITY vs VALUE_OBJECT), the result
 * is marked as CONFLICT.
 *
 * <p>This classifier uses the {@link CriteriaEngine} for evaluation and decision-making,
 * providing a consistent and extensible classification mechanism.
 */
public final class DomainClassifier {

    private final CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine;
    private final DecisionPolicy<DomainKind> decisionPolicy;
    private final CompatibilityPolicy<DomainKind> compatibilityPolicy;

    /**
     * Creates a classifier with the default set of criteria.
     */
    public DomainClassifier() {
        this(defaultCriteria(), CriteriaProfile.legacy());
    }

    /**
     * Creates a classifier with custom criteria (for testing).
     */
    public DomainClassifier(List<ClassificationCriteria<DomainKind>> criteria) {
        this(criteria, CriteriaProfile.legacy());
    }

    /**
     * Creates a classifier with custom criteria and profile.
     *
     * @param criteria the classification criteria to use
     * @param profile the profile for priority overrides
     */
    public DomainClassifier(List<ClassificationCriteria<DomainKind>> criteria, CriteriaProfile profile) {
        this.decisionPolicy = new DefaultDecisionPolicy<>();
        this.compatibilityPolicy = CompatibilityPolicy.domainDefault();
        this.engine = new CriteriaEngine<>(
                criteria, profile, decisionPolicy, compatibilityPolicy, DomainClassifier::buildContribution);
    }

    /**
     * Returns the default set of domain classification criteria.
     */
    public static List<ClassificationCriteria<DomainKind>> defaultCriteria() {
        return List.of(
                // Explicit annotations (priority 100)
                new ExplicitAggregateRootCriteria(),
                new ExplicitEntityCriteria(),
                new ExplicitValueObjectCriteria(),
                new ExplicitIdentifierCriteria(),
                new ExplicitDomainEventCriteria(),
                new ExplicitExternalizedEventCriteria(),
                // Explicit interfaces (priority 100)
                new ImplementsJMoleculesInterfaceCriteria(),
                // Strong heuristics (priority 80)
                new RepositoryDominantCriteria(),
                new RecordSingleIdCriteria(),
                // Inherited classification (priority 75)
                new InheritedClassificationCriteria(),
                // Medium heuristics (priority 70)
                new EmbeddedValueObjectCriteria(),
                // Relationship-based heuristics (priority 65)
                new HasPortDependenciesCriteria(),
                // Medium heuristics (priority 60)
                new HasIdentityCriteria(),
                new CollectionElementEntityCriteria(),
                new ImmutableNoIdCriteria(),
                // Naming heuristics (priority 55)
                new NamingDomainEventCriteria(),
                new StatelessNoDependenciesCriteria(),
                // Lower heuristics (priority 50)
                new UnreferencedInPortsCriteria());
    }

    /**
     * Classifies a type node.
     *
     * @param node the type to classify
     * @param query the graph query for context
     * @return the classification result
     */
    public ClassificationResult classify(TypeNode node, GraphQuery query) {
        // Evaluate criteria and collect contributions
        List<Contribution<DomainKind>> contributions = engine.evaluate(node, query);

        if (contributions.isEmpty()) {
            return ClassificationResult.unclassified(node.id());
        }

        // Delegate to decision policy
        DecisionPolicy.Decision<DomainKind> decision = decisionPolicy.decide(contributions, compatibilityPolicy);

        return toClassificationResult(node.id(), decision, contributions);
    }

    /**
     * Converts a Decision to a ClassificationResult.
     */
    private ClassificationResult toClassificationResult(
            NodeId nodeId, DecisionPolicy.Decision<DomainKind> decision, List<Contribution<DomainKind>> contributions) {

        if (decision.isEmpty()) {
            return ClassificationResult.unclassified(nodeId);
        }

        if (decision.hasIncompatibleConflict()) {
            return ClassificationResult.conflictDomain(nodeId, decision.conflicts());
        }

        Contribution<DomainKind> winner = decision.winner().orElseThrow();

        // Detect conflicts: other contributions with different kinds
        List<Conflict> conflicts = contributions.stream()
                .filter(c -> c != winner && c.kind() != winner.kind())
                .map(c -> {
                    boolean compatible = compatibilityPolicy.areCompatible(winner.kind(), c.kind());
                    ConflictSeverity severity = compatible ? ConflictSeverity.WARNING : ConflictSeverity.ERROR;
                    return new Conflict(
                            c.kind().name(),
                            c.criteriaName(),
                            c.confidence(),
                            c.priority(),
                            "Also matched with " + c.justification(),
                            severity);
                })
                .toList();

        return ClassificationResult.classified(
                nodeId,
                ClassificationTarget.DOMAIN,
                winner.kind().name(),
                winner.confidence(),
                winner.criteriaName(),
                winner.priority(),
                winner.justification(),
                winner.evidence(),
                conflicts);
    }

    /**
     * Builds a contribution from a criteria match result.
     */
    private static Contribution<DomainKind> buildContribution(
            ClassificationCriteria<DomainKind> criteria, MatchResult result, int priority) {
        return Contribution.of(
                criteria.targetKind(),
                criteria.name(),
                priority,
                result.confidence(),
                result.justification(),
                result.evidence());
    }
}
