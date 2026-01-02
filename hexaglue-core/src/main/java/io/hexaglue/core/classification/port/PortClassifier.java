package io.hexaglue.core.classification.port;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.criteria.CommandPatternCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitPrimaryPortCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitRepositoryCriteria;
import io.hexaglue.core.classification.port.criteria.ExplicitSecondaryPortCriteria;
import io.hexaglue.core.classification.port.criteria.NamingGatewayCriteria;
import io.hexaglue.core.classification.port.criteria.NamingRepositoryCriteria;
import io.hexaglue.core.classification.port.criteria.NamingUseCaseCriteria;
import io.hexaglue.core.classification.port.criteria.PackageInCriteria;
import io.hexaglue.core.classification.port.criteria.PackageOutCriteria;
import io.hexaglue.core.classification.port.criteria.QueryPatternCriteria;
import io.hexaglue.core.classification.port.criteria.SignatureBasedDrivenPortCriteria;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
 */
public final class PortClassifier {

    /**
     * Kinds that are considered compatible (can coexist without conflict).
     * Currently no port kinds are compatible - each interface is exactly one kind.
     */
    private static final Set<Set<PortKind>> COMPATIBLE_KINDS = Set.of();

    private final List<PortClassificationCriteria> criteria;

    /**
     * Creates a classifier with the default set of criteria.
     */
    public PortClassifier() {
        this(defaultCriteria());
    }

    /**
     * Creates a classifier with custom criteria (for testing).
     */
    public PortClassifier(List<PortClassificationCriteria> criteria) {
        this.criteria = List.copyOf(criteria);
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

        return ClassificationResult.classifiedPort(
                node.id(),
                winner.criteria().targetKind().name(),
                winner.result().confidence(),
                winner.criteria().name(),
                winner.criteria().priority(),
                winner.result().justification(),
                winner.result().evidence(),
                conflicts,
                winner.criteria().targetDirection());
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
        PortKind winnerKind = winner.criteria().targetKind();

        for (CriteriaMatch match : allMatches) {
            if (match == winner) {
                continue;
            }

            PortKind matchKind = match.criteria().targetKind();
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

        PortKind winnerKind = winner.criteria().targetKind();
        int winnerPriority = winner.criteria().priority();

        for (Conflict conflict : conflicts) {
            // Only consider as true conflict if same priority
            if (conflict.competingPriority() == winnerPriority) {
                PortKind conflictKind = PortKind.valueOf(conflict.competingKind());
                if (!areCompatible(winnerKind, conflictKind)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if two port kinds are compatible (can coexist).
     */
    private boolean areCompatible(PortKind a, PortKind b) {
        if (a == b) {
            return true;
        }
        for (Set<PortKind> compatible : COMPATIBLE_KINDS) {
            if (compatible.contains(a) && compatible.contains(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Internal record to pair a criteria with its match result.
     */
    private record CriteriaMatch(PortClassificationCriteria criteria, MatchResult result) {}
}
