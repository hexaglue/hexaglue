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

import io.hexaglue.core.classification.Conflict;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Default implementation of {@link DecisionPolicy}.
 *
 * <p>This policy implements the standard HexaGlue tie-breaking algorithm:
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence weight (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical order for determinism</li>
 * </ol>
 *
 * <p>Conflicts are detected when multiple contributions have different
 * kinds at the same priority level. If the kinds are incompatible
 * (according to the {@link CompatibilityPolicy}), the result is a
 * hard conflict with no winner.
 *
 * @param <K> the kind type (e.g., DomainKind, PortKind)
 */
public final class DefaultDecisionPolicy<K> implements DecisionPolicy<K> {

    @Override
    public Decision<K> decide(List<Contribution<K>> contributions, CompatibilityPolicy<K> compatibility) {
        if (contributions == null || contributions.isEmpty()) {
            return Decision.empty();
        }

        // Sort contributions by the standard tie-breaking algorithm
        List<Contribution<K>> sorted =
                contributions.stream().sorted(contributionComparator()).toList();

        Contribution<K> winner = sorted.get(0);

        // Check for incompatible conflicts at the same priority
        List<Contribution<K>> samePriority =
                sorted.stream().filter(c -> c.priority() == winner.priority()).toList();

        if (hasIncompatibleConflicts(winner, samePriority, compatibility)) {
            List<Conflict> conflicts = buildConflicts(samePriority);
            return Decision.conflict(conflicts);
        }

        // Detect conflicts (different kinds that matched)
        List<Conflict> conflicts = detectConflicts(winner, sorted, compatibility);

        if (conflicts.isEmpty()) {
            return Decision.success(winner);
        } else {
            return Decision.successWithConflicts(winner, conflicts);
        }
    }

    /**
     * Creates the standard comparator for tie-breaking.
     *
     * <p>Order: priority DESC → confidence weight DESC → name ASC
     */
    private Comparator<Contribution<K>> contributionComparator() {
        return Comparator
                // Priority descending (higher wins) - negate to reverse without using reversed()
                .comparingInt((Contribution<K> c) -> -c.priority())
                // Confidence weight descending (higher wins) - negate to reverse
                .thenComparingInt((Contribution<K> c) -> -c.confidence().weight())
                // Name ascending (for determinism)
                .thenComparing(Contribution::criteriaName);
    }

    /**
     * Checks if there are incompatible conflicts at the same priority level.
     */
    private boolean hasIncompatibleConflicts(
            Contribution<K> winner, List<Contribution<K>> samePriority, CompatibilityPolicy<K> compatibility) {

        for (Contribution<K> other : samePriority) {
            if (other == winner) {
                continue;
            }
            if (!other.kind().equals(winner.kind()) && !compatibility.areCompatible(winner.kind(), other.kind())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects all conflicts (contributions with different kinds).
     *
     * <p>Conflicts are categorized by severity:
     * <ul>
     *   <li>{@code ERROR}: Incompatible kinds (cannot coexist)</li>
     *   <li>{@code WARNING}: Compatible kinds (informational)</li>
     * </ul>
     */
    private List<Conflict> detectConflicts(
            Contribution<K> winner, List<Contribution<K>> all, CompatibilityPolicy<K> compatibility) {

        List<Conflict> conflicts = new ArrayList<>();

        for (Contribution<K> other : all) {
            if (other == winner) {
                continue;
            }
            if (!other.kind().equals(winner.kind())) {
                boolean compatible = compatibility.areCompatible(winner.kind(), other.kind());
                String rationale = "Also matched with " + other.justification();

                if (compatible) {
                    conflicts.add(Conflict.warning(
                            other.kind().toString(),
                            other.criteriaName(),
                            other.confidence(),
                            other.priority(),
                            rationale));
                } else {
                    conflicts.add(Conflict.error(
                            other.kind().toString(),
                            other.criteriaName(),
                            other.confidence(),
                            other.priority(),
                            rationale));
                }
            }
        }

        return conflicts;
    }

    /**
     * Builds conflict records for all contributions (used when there's a hard conflict).
     *
     * <p>All conflicts in this case are ERROR severity since they represent
     * incompatible kinds at the same priority level.
     */
    private List<Conflict> buildConflicts(List<Contribution<K>> contributions) {
        return contributions.stream()
                .map(c -> Conflict.error(
                        c.kind().toString(), c.criteriaName(), c.confidence(), c.priority(), c.justification()))
                .toList();
    }
}
