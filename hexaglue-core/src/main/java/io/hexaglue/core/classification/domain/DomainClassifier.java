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
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.criteria.CollectionElementEntityCriteria;
import io.hexaglue.core.classification.domain.criteria.EmbeddedValueObjectCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitAggregateRootCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitDomainEventCriteria;
import io.hexaglue.core.classification.domain.criteria.ExplicitEntityCriteria;
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
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
 */
public final class DomainClassifier {

    /**
     * Kinds that are considered compatible (can coexist without conflict).
     * AGGREGATE_ROOT is a special case of ENTITY.
     */
    private static final Set<Set<DomainKind>> COMPATIBLE_KINDS =
            Set.of(Set.of(DomainKind.AGGREGATE_ROOT, DomainKind.ENTITY));

    private final List<ClassificationCriteria<DomainKind>> criteria;

    /**
     * Creates a classifier with the default set of criteria.
     */
    public DomainClassifier() {
        this(defaultCriteria());
    }

    /**
     * Creates a classifier with custom criteria (for testing).
     */
    public DomainClassifier(List<ClassificationCriteria<DomainKind>> criteria) {
        this.criteria = List.copyOf(criteria);
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
        // Collect all matching criteria
        List<CriteriaMatch> matches = criteria.stream()
                .map(c -> new CriteriaMatch(c, c.evaluate(node, query)))
                .filter(m -> m.result().matched())
                .sorted(matchComparator())
                .toList();

        if (matches.isEmpty()) {
            return ClassificationResult.unclassified(node.id());
        }

        // Winner is the first after sorting
        CriteriaMatch winner = matches.get(0);

        // Detect conflicts with other matches
        List<Conflict> conflicts = detectConflicts(winner, matches);

        // Check for incompatible conflicts
        if (hasIncompatibleConflicts(winner, conflicts)) {
            return ClassificationResult.conflict(node.id(), conflicts);
        }

        return ClassificationResult.classified(
                node.id(),
                ClassificationTarget.DOMAIN,
                winner.criteria().targetKind().name(),
                winner.result().confidence(),
                winner.criteria().name(),
                winner.criteria().priority(),
                winner.result().justification(),
                winner.result().evidence(),
                conflicts);
    }

    /**
     * Deterministic comparator for tie-breaking.
     * Order: priority DESC → confidence DESC → name ASC
     */
    private Comparator<CriteriaMatch> matchComparator() {
        return Comparator
                // Priority descending (higher wins)
                .comparingInt((CriteriaMatch m) -> m.criteria().priority())
                .reversed()
                // Confidence descending (higher wins)
                .thenComparing((CriteriaMatch m) -> m.result().confidence(), Comparator.reverseOrder())
                // Name ascending (for determinism)
                .thenComparing(m -> m.criteria().name());
    }

    /**
     * Detects conflicts between the winner and other matching criteria.
     */
    private List<Conflict> detectConflicts(CriteriaMatch winner, List<CriteriaMatch> allMatches) {
        List<Conflict> conflicts = new ArrayList<>();
        DomainKind winnerKind = winner.criteria().targetKind();

        for (CriteriaMatch match : allMatches) {
            if (match == winner) {
                continue;
            }

            DomainKind matchKind = match.criteria().targetKind();
            if (matchKind != winnerKind) {
                conflicts.add(new Conflict(
                        matchKind.name(),
                        match.criteria().name(),
                        match.result().confidence(),
                        match.criteria().priority(),
                        "Also matched with " + match.result().justification()));
            }
        }

        return conflicts;
    }

    /**
     * Checks if any conflicts are incompatible with the winner AND have the same priority.
     *
     * <p>We only return CONFLICT if there's a real ambiguity - i.e., multiple criteria
     * with the same priority targeting incompatible kinds. If the winner has higher
     * priority, it wins even if there are incompatible lower-priority matches.
     */
    private boolean hasIncompatibleConflicts(CriteriaMatch winner, List<Conflict> conflicts) {
        if (conflicts.isEmpty()) {
            return false;
        }

        DomainKind winnerKind = winner.criteria().targetKind();
        int winnerPriority = winner.criteria().priority();

        for (Conflict conflict : conflicts) {
            // Only consider as true conflict if same priority
            if (conflict.competingPriority() == winnerPriority) {
                DomainKind conflictKind = DomainKind.valueOf(conflict.competingKind());
                if (!areCompatible(winnerKind, conflictKind)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if two domain kinds are compatible (can coexist).
     */
    private boolean areCompatible(DomainKind a, DomainKind b) {
        if (a == b) {
            return true;
        }
        for (Set<DomainKind> compatible : COMPATIBLE_KINDS) {
            if (compatible.contains(a) && compatible.contains(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Internal record to pair a criteria with its match result.
     */
    private record CriteriaMatch(ClassificationCriteria<DomainKind> criteria, MatchResult result) {}
}
