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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Policy for making classification decisions from contributions.
 *
 * <p>A decision policy takes a list of contributions and determines:
 * <ul>
 *   <li>The winner (if any)</li>
 *   <li>Any conflicts detected</li>
 *   <li>Whether the result is a hard conflict (incompatible kinds)</li>
 * </ul>
 *
 * @param <K> the kind type (e.g., ElementKind, PortKind)
 */
public interface DecisionPolicy<K> {

    /**
     * Makes a decision based on the provided contributions.
     *
     * @param contributions the contributions from matching criteria
     * @param compatibility the policy for determining kind compatibility
     * @return the decision result
     */
    Decision<K> decide(List<Contribution<K>> contributions, CompatibilityPolicy<K> compatibility);

    /**
     * The result of a classification decision.
     *
     * @param <K> the kind type
     * @param winner the winning contribution (empty if no match or hard conflict)
     * @param conflicts any conflicts detected during decision
     * @param hasIncompatibleConflict true if there were incompatible kinds at the same priority
     */
    record Decision<K>(Optional<Contribution<K>> winner, List<Conflict> conflicts, boolean hasIncompatibleConflict) {

        public Decision {
            Objects.requireNonNull(winner, "winner cannot be null (use Optional.empty())");
            conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        }

        /**
         * Creates a successful decision with a winner and no conflicts.
         *
         * @param winner the winning contribution
         * @param <K> the kind type
         * @return a successful decision
         */
        public static <K> Decision<K> success(Contribution<K> winner) {
            return new Decision<>(Optional.of(winner), List.of(), false);
        }

        /**
         * Creates a successful decision with a winner and some conflicts (warnings).
         *
         * @param winner the winning contribution
         * @param conflicts the conflicts detected (as warnings)
         * @param <K> the kind type
         * @return a successful decision with conflicts
         */
        public static <K> Decision<K> successWithConflicts(Contribution<K> winner, List<Conflict> conflicts) {
            return new Decision<>(Optional.of(winner), conflicts, false);
        }

        /**
         * Creates a conflict decision (no winner, incompatible kinds).
         *
         * @param conflicts the conflicting contributions
         * @param <K> the kind type
         * @return a conflict decision
         */
        public static <K> Decision<K> conflict(List<Conflict> conflicts) {
            return new Decision<>(Optional.empty(), conflicts, true);
        }

        /**
         * Creates an empty decision (no contributions matched).
         *
         * @param <K> the kind type
         * @return an empty decision
         */
        public static <K> Decision<K> empty() {
            return new Decision<>(Optional.empty(), List.of(), false);
        }

        /**
         * Returns true if this decision has a winner.
         */
        public boolean hasWinner() {
            return winner.isPresent();
        }

        /**
         * Returns true if this decision has conflicts.
         */
        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }

        /**
         * Returns true if this decision represents no match (empty contributions).
         */
        public boolean isEmpty() {
            return winner.isEmpty() && !hasIncompatibleConflict;
        }
    }
}
