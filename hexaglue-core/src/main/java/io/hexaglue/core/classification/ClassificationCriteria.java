package io.hexaglue.core.classification;

import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;

/**
 * A classification criteria that can evaluate whether a type matches
 * a particular classification kind.
 *
 * <p>Criteria are evaluated in priority order. When multiple criteria match,
 * the one with highest priority wins. Ties are broken by confidence level,
 * then by criteria name (alphabetically).
 *
 * <p>Example criteria:
 * <ul>
 *   <li>ExplicitAggregateRootCriteria - matches types with @AggregateRoot annotation</li>
 *   <li>RepositoryDominantCriteria - matches types used in Repository signatures with id field</li>
 *   <li>ImmutableNoIdCriteria - matches immutable types without id field (VALUE_OBJECT)</li>
 * </ul>
 *
 * @param <K> the kind enum type (e.g., DomainKind, PortKind)
 */
public interface ClassificationCriteria<K extends Enum<K>> {

    /**
     * Returns the unique name of this criteria.
     *
     * <p>Used for tie-breaking and diagnostics.
     * Examples: "explicit-annotation", "repository-dominant", "naming-pattern".
     */
    String name();

    /**
     * Returns the priority of this criteria.
     *
     * <p>Higher priority criteria are evaluated first.
     * Range: 0-100, where 100 is highest priority.
     *
     * <p>Guidelines:
     * <ul>
     *   <li>100: Explicit annotations (user declared intent)</li>
     *   <li>80: Strong heuristics (multiple signals)</li>
     *   <li>60: Medium heuristics (package + naming)</li>
     *   <li>40: Weak heuristics (naming only)</li>
     * </ul>
     */
    int priority();

    /**
     * Returns the classification kind this criteria targets.
     *
     * <p>Examples: DomainKind.AGGREGATE_ROOT, PortKind.REPOSITORY
     */
    K targetKind();

    /**
     * Evaluates whether the given type matches this criteria.
     *
     * @param node the type node to evaluate
     * @param query the graph query for accessing related information
     * @return the match result (matched or no-match with confidence and evidence)
     */
    MatchResult evaluate(TypeNode node, GraphQuery query);

    /**
     * Returns a human-readable description of this criteria.
     *
     * <p>Used for documentation and diagnostics.
     */
    default String description() {
        return name() + " -> " + targetKind();
    }
}
