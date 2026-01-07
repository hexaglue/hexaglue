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

package io.hexaglue.core.classification.port;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.classification.ConflictSeverity;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.engine.CompatibilityPolicy;
import io.hexaglue.core.classification.engine.Contribution;
import io.hexaglue.core.classification.engine.CriteriaEngine;
import io.hexaglue.core.classification.engine.CriteriaProfile;
import io.hexaglue.core.classification.engine.DecisionPolicy;
import io.hexaglue.core.classification.engine.DefaultDecisionPolicy;
import io.hexaglue.core.classification.port.criteria.CommandPatternCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitPrimaryPortCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitRepositoryCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitSecondaryPortCriteria;
import io.hexaglue.core.classification.port.criteria.InjectedAsDependencyCriteria;
import io.hexaglue.core.classification.port.criteria.NamingGatewayCriteria;
import io.hexaglue.core.classification.port.criteria.NamingRepositoryCriteria;
import io.hexaglue.core.classification.port.criteria.NamingUseCaseCriteria;
import io.hexaglue.core.classification.port.criteria.PackageInCriteria;
import io.hexaglue.core.classification.port.criteria.PackageOutCriteria;
import io.hexaglue.core.classification.port.criteria.QueryPatternCriteria;
import io.hexaglue.core.classification.port.criteria.SignatureBasedDrivenPortCriteria;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Classifies types as port interfaces (REPOSITORY, USE_CASE, GATEWAY, etc.).
 *
 * <p>The classifier evaluates all registered criteria against a type and selects
 * the best match using a deterministic tie-break algorithm:
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical order for determinism</li>
 * </ol>
 *
 * <p>Ports are only classified for interface types. Classes, records, and enums
 * are not considered ports.
 *
 * <p>Conflicts are detected when multiple criteria match with different target kinds.
 * If the conflicts are incompatible (e.g., USE_CASE vs REPOSITORY), the result
 * is marked as CONFLICT.
 *
 * <p>This classifier uses the {@link CriteriaEngine} for evaluation and decision-making,
 * providing a consistent and extensible classification mechanism.
 */
public final class PortClassifier {

    /** Metadata key for storing port direction in contributions. */
    private static final String DIRECTION_KEY = "direction";

    private final CriteriaEngine<PortKind, PortClassificationCriteria> engine;
    private final DecisionPolicy<PortKind> decisionPolicy;
    private final CompatibilityPolicy<PortKind> compatibilityPolicy;

    /**
     * Creates a classifier with the default set of criteria.
     */
    public PortClassifier() {
        this(defaultCriteria(), CriteriaProfile.legacy());
    }

    /**
     * Creates a classifier with custom criteria (for testing).
     */
    public PortClassifier(List<PortClassificationCriteria> criteria) {
        this(criteria, CriteriaProfile.legacy());
    }

    /**
     * Creates a classifier with custom criteria and profile.
     *
     * @param criteria the classification criteria to use
     * @param profile the profile for priority overrides
     */
    public PortClassifier(List<PortClassificationCriteria> criteria, CriteriaProfile profile) {
        this.decisionPolicy = new DefaultDecisionPolicy<>();
        this.compatibilityPolicy = CompatibilityPolicy.portDefault();
        this.engine = new CriteriaEngine<>(
                criteria, profile, decisionPolicy, compatibilityPolicy, PortClassifier::buildContribution);
    }

    /**
     * Returns the default set of port classification criteria.
     */
    public static List<PortClassificationCriteria> defaultCriteria() {
        return List.of(
                // Explicit annotations (priority 100)
                new ExplicitRepositoryCriteria(),
                new ExplicitPrimaryPortCriteria(),
                new ExplicitSecondaryPortCriteria(),
                // Strong heuristics (priority 80)
                new NamingRepositoryCriteria(),
                new NamingUseCaseCriteria(),
                new NamingGatewayCriteria(),
                // CQRS pattern heuristics (priority 75)
                new CommandPatternCriteria(),
                new QueryPatternCriteria(),
                // Relationship-based heuristics (priority 75)
                new InjectedAsDependencyCriteria(),
                // Graph-based heuristics (priority 70)
                new SignatureBasedDrivenPortCriteria(),
                // Medium heuristics (priority 60)
                new PackageInCriteria(),
                new PackageOutCriteria());
    }

    /**
     * Classifies a type node as a port.
     *
     * <p>Only interface types can be classified as ports. Other types
     * (classes, records, enums) will return an unclassified result.
     *
     * @param node the type to classify
     * @param query the graph query for context
     * @return the classification result
     */
    public ClassificationResult classify(TypeNode node, GraphQuery query) {
        // Only interfaces can be ports
        if (!node.isInterface()) {
            return ClassificationResult.unclassified(node.id());
        }

        // Evaluate criteria and collect contributions
        List<Contribution<PortKind>> contributions = engine.evaluate(node, query);

        if (contributions.isEmpty()) {
            return ClassificationResult.unclassified(node.id());
        }

        // Delegate to decision policy
        DecisionPolicy.Decision<PortKind> decision = decisionPolicy.decide(contributions, compatibilityPolicy);

        return toClassificationResult(node.id(), decision, contributions);
    }

    /**
     * Converts a Decision to a ClassificationResult.
     */
    private ClassificationResult toClassificationResult(
            NodeId nodeId, DecisionPolicy.Decision<PortKind> decision, List<Contribution<PortKind>> contributions) {

        if (decision.isEmpty()) {
            return ClassificationResult.unclassified(nodeId);
        }

        if (decision.hasIncompatibleConflict()) {
            return ClassificationResult.conflictPort(nodeId, decision.conflicts());
        }

        Contribution<PortKind> winner = decision.winner().orElseThrow();

        // Extract direction from metadata
        PortDirection direction = winner.metadata(DIRECTION_KEY, PortDirection.class)
                .orElseThrow(() -> new IllegalStateException("Port direction not found in contribution metadata"));

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

        return ClassificationResult.classifiedPort(
                nodeId,
                winner.kind().name(),
                winner.confidence(),
                winner.criteriaName(),
                winner.priority(),
                winner.justification(),
                winner.evidence(),
                conflicts,
                direction);
    }

    /**
     * Builds a contribution from a criteria match result.
     *
     * <p>The port direction is stored in the contribution's metadata.
     */
    private static Contribution<PortKind> buildContribution(
            PortClassificationCriteria criteria, MatchResult result, int priority) {
        return Contribution.of(
                        criteria.targetKind(),
                        criteria.name(),
                        priority,
                        result.confidence(),
                        result.justification(),
                        result.evidence())
                .withMetadata(DIRECTION_KEY, criteria.targetDirection());
    }
}
