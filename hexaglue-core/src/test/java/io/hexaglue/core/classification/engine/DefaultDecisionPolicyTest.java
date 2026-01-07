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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.domain.DomainKind;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultDecisionPolicy}.
 *
 * <p>Verifies the tie-breaking algorithm:
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence weight (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical order for determinism</li>
 * </ol>
 */
@DisplayName("DefaultDecisionPolicy")
class DefaultDecisionPolicyTest {

    private DefaultDecisionPolicy<DomainKind> policy;
    private CompatibilityPolicy<DomainKind> noneCompatible;
    private CompatibilityPolicy<DomainKind> domainCompatible;

    @BeforeEach
    void setUp() {
        policy = new DefaultDecisionPolicy<>();
        noneCompatible = CompatibilityPolicy.noneCompatible();
        domainCompatible = CompatibilityPolicy.domainDefault();
    }

    // =========================================================================
    // Empty Input
    // =========================================================================

    @Nested
    @DisplayName("Empty input")
    class EmptyInputTest {

        @Test
        @DisplayName("should return empty decision for null contributions")
        void shouldReturnEmptyForNull() {
            DecisionPolicy.Decision<DomainKind> decision = policy.decide(null, noneCompatible);

            assertThat(decision.isEmpty()).isTrue();
            assertThat(decision.hasWinner()).isFalse();
            assertThat(decision.hasConflicts()).isFalse();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
        }

        @Test
        @DisplayName("should return empty decision for empty list")
        void shouldReturnEmptyForEmptyList() {
            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(), noneCompatible);

            assertThat(decision.isEmpty()).isTrue();
            assertThat(decision.hasWinner()).isFalse();
        }
    }

    // =========================================================================
    // Single Contribution
    // =========================================================================

    @Nested
    @DisplayName("Single contribution")
    class SingleContributionTest {

        @Test
        @DisplayName("should return winner with no conflicts")
        void shouldReturnWinnerNoConflicts() {
            Contribution<DomainKind> contribution = Contribution.of(
                    DomainKind.AGGREGATE_ROOT, "explicit-ar", 100, ConfidenceLevel.EXPLICIT, "Has @AggregateRoot");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(contribution), noneCompatible);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.winner()).isPresent().hasValue(contribution);
            assertThat(decision.hasConflicts()).isFalse();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
        }
    }

    // =========================================================================
    // Priority Dominates (Contract: priority DESC first)
    // =========================================================================

    @Nested
    @DisplayName("Priority dominates")
    class PriorityDominatesTest {

        @Test
        @DisplayName("higher priority should win regardless of confidence")
        void higherPriorityShouldWin() {
            Contribution<DomainKind> lowPriorityExplicit =
                    Contribution.of(DomainKind.VALUE_OBJECT, "low-priority", 50, ConfidenceLevel.EXPLICIT, "Explicit");
            Contribution<DomainKind> highPriorityLow =
                    Contribution.of(DomainKind.ENTITY, "high-priority", 100, ConfidenceLevel.LOW, "Low confidence");

            DecisionPolicy.Decision<DomainKind> decision =
                    policy.decide(List.of(lowPriorityExplicit, highPriorityLow), noneCompatible);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.winner().get().kind()).isEqualTo(DomainKind.ENTITY);
            assertThat(decision.winner().get().priority()).isEqualTo(100);
        }

        @Test
        @DisplayName("priority 100 LOW should beat priority 50 EXPLICIT")
        void priority100LowBeatsPriority50Explicit() {
            Contribution<DomainKind> p50Explicit = Contribution.of(
                    DomainKind.VALUE_OBJECT, "p50", 50, ConfidenceLevel.EXPLICIT, "Priority 50 EXPLICIT");
            Contribution<DomainKind> p100Low =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "p100", 100, ConfidenceLevel.LOW, "Priority 100 LOW");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(p50Explicit, p100Low), noneCompatible);

            assertThat(decision.winner().get().criteriaName()).isEqualTo("p100");
            assertThat(decision.winner().get().confidence()).isEqualTo(ConfidenceLevel.LOW);
        }

        @Test
        @DisplayName("order of contributions should not matter")
        void orderShouldNotMatter() {
            Contribution<DomainKind> p80 =
                    Contribution.of(DomainKind.ENTITY, "p80", 80, ConfidenceLevel.HIGH, "Priority 80");
            Contribution<DomainKind> p100 =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "p100", 100, ConfidenceLevel.MEDIUM, "Priority 100");

            DecisionPolicy.Decision<DomainKind> decision1 = policy.decide(List.of(p80, p100), noneCompatible);
            DecisionPolicy.Decision<DomainKind> decision2 = policy.decide(List.of(p100, p80), noneCompatible);

            assertThat(decision1.winner().get().criteriaName()).isEqualTo("p100");
            assertThat(decision2.winner().get().criteriaName()).isEqualTo("p100");
        }
    }

    // =========================================================================
    // Same Priority Uses Confidence (Contract: confidence.weight() DESC)
    // =========================================================================

    @Nested
    @DisplayName("Same priority uses confidence weight")
    class SamePriorityUsesConfidenceTest {

        @Test
        @DisplayName("higher confidence weight should win when priority is equal")
        void higherConfidenceWeightShouldWin() {
            // Same priority, same kind - confidence should decide
            Contribution<DomainKind> lowConfidence =
                    Contribution.of(DomainKind.ENTITY, "low-conf", 80, ConfidenceLevel.LOW, "Low confidence");
            Contribution<DomainKind> highConfidence =
                    Contribution.of(DomainKind.ENTITY, "high-conf", 80, ConfidenceLevel.HIGH, "High confidence");

            DecisionPolicy.Decision<DomainKind> decision =
                    policy.decide(List.of(lowConfidence, highConfidence), noneCompatible);

            assertThat(decision.winner().get().criteriaName()).isEqualTo("high-conf");
            assertThat(decision.winner().get().confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("EXPLICIT should beat HIGH at same priority")
        void explicitShouldBeatHigh() {
            Contribution<DomainKind> high =
                    Contribution.of(DomainKind.ENTITY, "high", 100, ConfidenceLevel.HIGH, "High");
            Contribution<DomainKind> explicit =
                    Contribution.of(DomainKind.ENTITY, "explicit", 100, ConfidenceLevel.EXPLICIT, "Explicit");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(high, explicit), noneCompatible);

            assertThat(decision.winner().get().criteriaName()).isEqualTo("explicit");
        }

        @Test
        @DisplayName("confidence ordering: EXPLICIT > HIGH > MEDIUM > LOW")
        void confidenceOrderingShouldRespectWeights() {
            Contribution<DomainKind> low = Contribution.of(DomainKind.ENTITY, "z-low", 80, ConfidenceLevel.LOW, "Low");
            Contribution<DomainKind> medium =
                    Contribution.of(DomainKind.ENTITY, "z-medium", 80, ConfidenceLevel.MEDIUM, "Medium");
            Contribution<DomainKind> high =
                    Contribution.of(DomainKind.ENTITY, "z-high", 80, ConfidenceLevel.HIGH, "High");
            Contribution<DomainKind> explicit =
                    Contribution.of(DomainKind.ENTITY, "z-explicit", 80, ConfidenceLevel.EXPLICIT, "Explicit");

            // Test in various orders
            List<Contribution<DomainKind>> order1 = List.of(low, medium, high, explicit);
            List<Contribution<DomainKind>> order2 = List.of(explicit, high, medium, low);
            List<Contribution<DomainKind>> order3 = List.of(medium, low, explicit, high);

            assertThat(policy.decide(order1, noneCompatible).winner().get().criteriaName())
                    .isEqualTo("z-explicit");
            assertThat(policy.decide(order2, noneCompatible).winner().get().criteriaName())
                    .isEqualTo("z-explicit");
            assertThat(policy.decide(order3, noneCompatible).winner().get().criteriaName())
                    .isEqualTo("z-explicit");
        }
    }

    // =========================================================================
    // Same Priority and Confidence Uses Name (Contract: name ASC for determinism)
    // =========================================================================

    @Nested
    @DisplayName("Same priority and confidence uses name")
    class SamePriorityAndConfidenceUsesNameTest {

        @Test
        @DisplayName("alphabetically first name should win")
        void alphabeticallyFirstShouldWin() {
            Contribution<DomainKind> zCriteria =
                    Contribution.of(DomainKind.ENTITY, "z-criteria", 80, ConfidenceLevel.HIGH, "Z");
            Contribution<DomainKind> aCriteria =
                    Contribution.of(DomainKind.ENTITY, "a-criteria", 80, ConfidenceLevel.HIGH, "A");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(zCriteria, aCriteria), noneCompatible);

            assertThat(decision.winner().get().criteriaName()).isEqualTo("a-criteria");
        }

        @Test
        @DisplayName("name comparison should be case-sensitive")
        void nameComparisonShouldBeCaseSensitive() {
            Contribution<DomainKind> upper =
                    Contribution.of(DomainKind.ENTITY, "A-criteria", 80, ConfidenceLevel.HIGH, "Upper");
            Contribution<DomainKind> lower =
                    Contribution.of(DomainKind.ENTITY, "a-criteria", 80, ConfidenceLevel.HIGH, "Lower");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(upper, lower), noneCompatible);

            // Uppercase 'A' comes before lowercase 'a' in ASCII
            assertThat(decision.winner().get().criteriaName()).isEqualTo("A-criteria");
        }
    }

    // =========================================================================
    // Conflict Detection
    // =========================================================================

    @Nested
    @DisplayName("Conflict detection")
    class ConflictDetectionTest {

        @Test
        @DisplayName("should detect conflicts when different kinds match")
        void shouldDetectConflicts() {
            Contribution<DomainKind> winner =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "winner", 100, ConfidenceLevel.EXPLICIT, "Winner");
            Contribution<DomainKind> loser =
                    Contribution.of(DomainKind.VALUE_OBJECT, "loser", 80, ConfidenceLevel.HIGH, "Loser");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(winner, loser), noneCompatible);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.hasConflicts()).isTrue();
            assertThat(decision.conflicts()).hasSize(1);
            assertThat(decision.conflicts().get(0).competingKind()).isEqualTo("VALUE_OBJECT");
        }

        @Test
        @DisplayName("should not detect conflicts when same kinds match")
        void shouldNotDetectConflictsForSameKind() {
            Contribution<DomainKind> winner =
                    Contribution.of(DomainKind.ENTITY, "winner", 100, ConfidenceLevel.EXPLICIT, "Winner");
            Contribution<DomainKind> other =
                    Contribution.of(DomainKind.ENTITY, "other", 80, ConfidenceLevel.HIGH, "Same kind");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(winner, other), noneCompatible);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.hasConflicts()).isFalse();
        }

        @Test
        @DisplayName("conflicts should include correct metadata")
        void conflictsShouldIncludeMetadata() {
            Contribution<DomainKind> winner =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "winner", 100, ConfidenceLevel.EXPLICIT, "Winner");
            Contribution<DomainKind> loser =
                    Contribution.of(DomainKind.VALUE_OBJECT, "loser", 80, ConfidenceLevel.HIGH, "Loser reason");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(winner, loser), noneCompatible);

            var conflict = decision.conflicts().get(0);
            assertThat(conflict.competingKind()).isEqualTo("VALUE_OBJECT");
            assertThat(conflict.competingCriteria()).isEqualTo("loser");
            assertThat(conflict.competingConfidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(conflict.competingPriority()).isEqualTo(80);
        }
    }

    // =========================================================================
    // Incompatible Conflicts at Same Priority
    // =========================================================================

    @Nested
    @DisplayName("Incompatible conflicts at same priority")
    class IncompatibleConflictsTest {

        @Test
        @DisplayName("should return conflict decision when incompatible kinds at same priority")
        void shouldReturnConflictForIncompatibleSamePriority() {
            Contribution<DomainKind> entity =
                    Contribution.of(DomainKind.ENTITY, "entity", 100, ConfidenceLevel.EXPLICIT, "Entity");
            Contribution<DomainKind> valueObject =
                    Contribution.of(DomainKind.VALUE_OBJECT, "vo", 100, ConfidenceLevel.EXPLICIT, "Value Object");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(entity, valueObject), noneCompatible);

            assertThat(decision.hasWinner()).isFalse();
            assertThat(decision.hasIncompatibleConflict()).isTrue();
            assertThat(decision.conflicts()).isNotEmpty();
        }

        @Test
        @DisplayName("should not return conflict when compatible kinds at same priority")
        void shouldNotConflictForCompatibleSamePriority() {
            // AGGREGATE_ROOT and ENTITY are compatible in domain classification
            Contribution<DomainKind> ar =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "ar", 100, ConfidenceLevel.EXPLICIT, "AR");
            Contribution<DomainKind> entity =
                    Contribution.of(DomainKind.ENTITY, "entity", 100, ConfidenceLevel.EXPLICIT, "Entity");

            DecisionPolicy.Decision<DomainKind> decision = policy.decide(List.of(ar, entity), domainCompatible);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
        }

        @Test
        @DisplayName("incompatible at lower priority should not cause hard conflict")
        void incompatibleAtLowerPriorityShouldNotCauseHardConflict() {
            Contribution<DomainKind> winner =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "winner", 100, ConfidenceLevel.EXPLICIT, "Winner");
            Contribution<DomainKind> loser1 =
                    Contribution.of(DomainKind.ENTITY, "loser1", 80, ConfidenceLevel.HIGH, "Loser 1");
            Contribution<DomainKind> loser2 =
                    Contribution.of(DomainKind.VALUE_OBJECT, "loser2", 80, ConfidenceLevel.HIGH, "Loser 2");

            DecisionPolicy.Decision<DomainKind> decision =
                    policy.decide(List.of(winner, loser1, loser2), noneCompatible);

            // Even though loser1 and loser2 are incompatible with each other,
            // the winner at priority 100 should be selected without hard conflict
            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
            assertThat(decision.winner().get().criteriaName()).isEqualTo("winner");
        }
    }

    // =========================================================================
    // Determinism (Contract: 100 runs → same result)
    // =========================================================================

    @Nested
    @DisplayName("Determinism")
    class DeterminismTest {

        @Test
        @DisplayName("100 runs should produce identical results")
        void hundredRunsShouldProduceSameResult() {
            Contribution<DomainKind> c1 =
                    Contribution.of(DomainKind.AGGREGATE_ROOT, "criteria-a", 100, ConfidenceLevel.EXPLICIT, "A");
            Contribution<DomainKind> c2 =
                    Contribution.of(DomainKind.ENTITY, "criteria-b", 100, ConfidenceLevel.EXPLICIT, "B");
            Contribution<DomainKind> c3 =
                    Contribution.of(DomainKind.VALUE_OBJECT, "criteria-c", 80, ConfidenceLevel.HIGH, "C");

            List<Contribution<DomainKind>> contributions = List.of(c1, c2, c3);

            DecisionPolicy.Decision<DomainKind> firstResult = policy.decide(contributions, noneCompatible);

            for (int i = 0; i < 100; i++) {
                DecisionPolicy.Decision<DomainKind> result = policy.decide(contributions, noneCompatible);

                assertThat(result.hasWinner()).as("Run %d: hasWinner", i).isEqualTo(firstResult.hasWinner());
                assertThat(result.hasIncompatibleConflict())
                        .as("Run %d: hasIncompatibleConflict", i)
                        .isEqualTo(firstResult.hasIncompatibleConflict());

                if (result.hasWinner()) {
                    assertThat(result.winner().get().criteriaName())
                            .as("Run %d: winner name", i)
                            .isEqualTo(firstResult.winner().get().criteriaName());
                }
            }
        }

        @Test
        @DisplayName("different orderings should produce same result")
        void differentOrderingsShouldProduceSameResult() {
            Contribution<DomainKind> c1 =
                    Contribution.of(DomainKind.ENTITY, "alpha", 80, ConfidenceLevel.HIGH, "Alpha");
            Contribution<DomainKind> c2 = Contribution.of(DomainKind.ENTITY, "beta", 80, ConfidenceLevel.HIGH, "Beta");
            Contribution<DomainKind> c3 =
                    Contribution.of(DomainKind.ENTITY, "gamma", 80, ConfidenceLevel.HIGH, "Gamma");

            // All possible orderings
            List<List<Contribution<DomainKind>>> orderings = List.of(
                    List.of(c1, c2, c3),
                    List.of(c1, c3, c2),
                    List.of(c2, c1, c3),
                    List.of(c2, c3, c1),
                    List.of(c3, c1, c2),
                    List.of(c3, c2, c1));

            String expectedWinner = "alpha"; // Alphabetically first

            for (List<Contribution<DomainKind>> ordering : orderings) {
                DecisionPolicy.Decision<DomainKind> result = policy.decide(ordering, noneCompatible);
                assertThat(result.winner().get().criteriaName())
                        .as(
                                "Ordering: %s",
                                ordering.stream()
                                        .map(Contribution::criteriaName)
                                        .toList())
                        .isEqualTo(expectedWinner);
            }
        }
    }

    // =========================================================================
    // Decision Record Helper Methods
    // =========================================================================

    @Nested
    @DisplayName("Decision helper methods")
    class DecisionHelperMethodsTest {

        @Test
        @DisplayName("Decision.success() should create correct decision")
        void successShouldCreateCorrectDecision() {
            Contribution<DomainKind> winner =
                    Contribution.of(DomainKind.ENTITY, "test", 80, ConfidenceLevel.HIGH, "Test");

            DecisionPolicy.Decision<DomainKind> decision = DecisionPolicy.Decision.success(winner);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.winner()).contains(winner);
            assertThat(decision.hasConflicts()).isFalse();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
            assertThat(decision.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Decision.empty() should create correct decision")
        void emptyShouldCreateCorrectDecision() {
            DecisionPolicy.Decision<DomainKind> decision = DecisionPolicy.Decision.empty();

            assertThat(decision.hasWinner()).isFalse();
            assertThat(decision.winner()).isEmpty();
            assertThat(decision.hasConflicts()).isFalse();
            assertThat(decision.hasIncompatibleConflict()).isFalse();
            assertThat(decision.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Decision.conflict() should create correct decision")
        void conflictShouldCreateCorrectDecision() {
            DecisionPolicy.Decision<DomainKind> decision =
                    DecisionPolicy.Decision.conflict(List.of(io.hexaglue.core.classification.Conflict.error(
                            "VALUE_OBJECT", "test", ConfidenceLevel.HIGH, 80, "Conflict")));

            assertThat(decision.hasWinner()).isFalse();
            assertThat(decision.winner()).isEmpty();
            assertThat(decision.hasConflicts()).isTrue();
            assertThat(decision.hasIncompatibleConflict()).isTrue();
            assertThat(decision.isEmpty()).isFalse();
        }
    }
}
