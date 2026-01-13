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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CriteriaEngine}.
 *
 * <p>Tests the engine's ability to:
 * <ul>
 *   <li>Evaluate criteria against type nodes</li>
 *   <li>Collect contributions from matching criteria</li>
 *   <li>Delegate to decision policy for classification</li>
 * </ul>
 */
@DisplayName("CriteriaEngine")
class CriteriaEngineTest {

    private TypeNode testNode;
    private GraphQuery mockQuery;
    private CompatibilityPolicy<DomainKind> compatibilityPolicy;

    @BeforeEach
    void setUp() {
        testNode = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .form(JavaForm.CLASS)
                .build();

        // GraphQuery can be null for simple tests as our mock criteria don't use it
        mockQuery = null;

        compatibilityPolicy = CompatibilityPolicy.domainDefault();
    }

    // =========================================================================
    // Helper Methods (at class level to avoid type inference issues)
    // =========================================================================

    private CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> createEngine(
            List<ClassificationCriteria<DomainKind>> criteria) {
        return new CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>>(
                criteria, new DefaultDecisionPolicy<>(), compatibilityPolicy, this::buildContribution);
    }

    private Contribution<DomainKind> buildContribution(
            ClassificationCriteria<DomainKind> criteria, MatchResult result, int priority) {
        return Contribution.of(
                criteria.targetKind(),
                criteria.name(),
                priority,
                result.confidence(),
                result.justification(),
                result.evidence());
    }

    @FunctionalInterface
    private interface NodeEvaluator {
        MatchResult evaluate(TypeNode node);
    }

    private ClassificationCriteria<DomainKind> createCriteria(
            String name, int priority, DomainKind targetKind, NodeEvaluator evaluator) {

        return new ClassificationCriteria<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public DomainKind targetKind() {
                return targetKind;
            }

            @Override
            public MatchResult evaluate(TypeNode node, GraphQuery query) {
                return evaluator.evaluate(node);
            }
        };
    }

    // =========================================================================
    // Construction
    // =========================================================================

    @Nested
    @DisplayName("Construction")
    class ConstructionTest {

        @Test
        @DisplayName("should reject null criteria list")
        void shouldRejectNullCriteria() {
            assertThatThrownBy(() -> new CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>>(
                            null,
                            new DefaultDecisionPolicy<>(),
                            compatibilityPolicy,
                            CriteriaEngineTest.this::buildContribution))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("criteria");
        }

        @Test
        @DisplayName("should reject null compatibility policy")
        void shouldRejectNullCompatibilityPolicy() {
            assertThatThrownBy(() -> new CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>>(
                            List.of(),
                            new DefaultDecisionPolicy<>(),
                            null,
                            CriteriaEngineTest.this::buildContribution))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("compatibilityPolicy");
        }

        @Test
        @DisplayName("should reject null contribution builder")
        void shouldRejectNullContributionBuilder() {
            assertThatThrownBy(() -> new CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>>(
                            List.of(),
                            new DefaultDecisionPolicy<>(),
                            compatibilityPolicy,
                            null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("contributionBuilder");
        }

        @Test
        @DisplayName("should create engine with empty criteria list")
        void shouldCreateWithEmptyCriteria() {
            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine = createEngine(List.of());

            assertThat(engine.criteria()).isEmpty();
        }
    }

    // =========================================================================
    // Evaluate
    // =========================================================================

    @Nested
    @DisplayName("Evaluate")
    class EvaluateTest {

        @Test
        @DisplayName("should return empty list when no criteria match")
        void shouldReturnEmptyWhenNoCriteriaMatch() {
            ClassificationCriteria<DomainKind> nonMatchingCriteria =
                    createCriteria("non-matching", 80, DomainKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine =
                    createEngine(List.of(nonMatchingCriteria));

            List<Contribution<DomainKind>> contributions = engine.evaluate(testNode, mockQuery);

            assertThat(contributions).isEmpty();
        }

        @Test
        @DisplayName("should return contributions from matching criteria")
        void shouldReturnContributionsFromMatchingCriteria() {
            ClassificationCriteria<DomainKind> matching = createCriteria(
                    "matching",
                    80,
                    DomainKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.HIGH, "Has repository"));

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine = createEngine(List.of(matching));

            List<Contribution<DomainKind>> contributions = engine.evaluate(testNode, mockQuery);

            assertThat(contributions).hasSize(1);
            assertThat(contributions.get(0).kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(contributions.get(0).criteriaName()).isEqualTo("matching");
        }

        @Test
        @DisplayName("should evaluate all criteria and collect matching ones")
        void shouldEvaluateAllCriteria() {
            ClassificationCriteria<DomainKind> match1 = createCriteria(
                    "match1", 100, DomainKind.AGGREGATE_ROOT, node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "A"));
            ClassificationCriteria<DomainKind> noMatch =
                    createCriteria("noMatch", 90, DomainKind.ENTITY, node -> MatchResult.noMatch());
            ClassificationCriteria<DomainKind> match2 = createCriteria(
                    "match2", 80, DomainKind.VALUE_OBJECT, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine =
                    createEngine(List.of(match1, noMatch, match2));

            List<Contribution<DomainKind>> contributions = engine.evaluate(testNode, mockQuery);

            assertThat(contributions).hasSize(2);
            assertThat(contributions)
                    .extracting(Contribution::criteriaName)
                    .containsExactlyInAnyOrder("match1", "match2");
        }
    }

    // =========================================================================
    // Classify
    // =========================================================================

    @Nested
    @DisplayName("Classify")
    class ClassifyTest {

        @Test
        @DisplayName("should return empty decision when no criteria match")
        void shouldReturnEmptyWhenNoCriteriaMatch() {
            ClassificationCriteria<DomainKind> noMatch =
                    createCriteria("noMatch", 80, DomainKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine = createEngine(List.of(noMatch));

            DecisionPolicy.Decision<DomainKind> decision = engine.classify(testNode, mockQuery);

            assertThat(decision.isEmpty()).isTrue();
            assertThat(decision.hasWinner()).isFalse();
        }

        @Test
        @DisplayName("should return winner based on priority")
        void shouldReturnWinnerBasedOnPriority() {
            ClassificationCriteria<DomainKind> highPriority = createCriteria(
                    "highPriority",
                    100,
                    DomainKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.LOW, "A"));
            ClassificationCriteria<DomainKind> lowPriority = createCriteria(
                    "lowPriority", 50, DomainKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "B"));

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine =
                    createEngine(List.of(lowPriority, highPriority));

            DecisionPolicy.Decision<DomainKind> decision = engine.classify(testNode, mockQuery);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.winner().get().kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(decision.winner().get().priority()).isEqualTo(100);
        }

        @Test
        @DisplayName("should detect conflicts with different kinds")
        void shouldDetectConflicts() {
            ClassificationCriteria<DomainKind> criteria1 = createCriteria(
                    "criteria1",
                    100,
                    DomainKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "A"));
            ClassificationCriteria<DomainKind> criteria2 = createCriteria(
                    "criteria2", 80, DomainKind.VALUE_OBJECT, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine =
                    createEngine(List.of(criteria1, criteria2));

            DecisionPolicy.Decision<DomainKind> decision = engine.classify(testNode, mockQuery);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.hasConflicts()).isTrue();
            assertThat(decision.conflicts()).hasSize(1);
            assertThat(decision.conflicts().get(0).competingKind()).isEqualTo("VALUE_OBJECT");
        }
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    @Nested
    @DisplayName("Accessors")
    class AccessorsTest {

        @Test
        @DisplayName("criteria() should return immutable list")
        void criteriaShouldBeImmutable() {
            ClassificationCriteria<DomainKind> criteria =
                    createCriteria("test", 80, DomainKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine = createEngine(List.of(criteria));

            assertThatThrownBy(() -> engine.criteria().add(criteria)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Nested
    @DisplayName("Determinism")
    class DeterminismTest {

        @Test
        @DisplayName("100 evaluations should produce identical results")
        void hundredEvaluationsShouldProduceSameResult() {
            ClassificationCriteria<DomainKind> criteria1 = createCriteria(
                    "criteria1", 80, DomainKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.HIGH, "A"));
            ClassificationCriteria<DomainKind> criteria2 = createCriteria(
                    "criteria2", 80, DomainKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<DomainKind, ClassificationCriteria<DomainKind>> engine =
                    createEngine(List.of(criteria1, criteria2));

            DecisionPolicy.Decision<DomainKind> firstResult = engine.classify(testNode, mockQuery);

            for (int i = 0; i < 100; i++) {
                DecisionPolicy.Decision<DomainKind> result = engine.classify(testNode, mockQuery);

                assertThat(result.hasWinner()).as("Run %d: hasWinner", i).isEqualTo(firstResult.hasWinner());
                if (result.hasWinner()) {
                    assertThat(result.winner().get().criteriaName())
                            .as("Run %d: winner", i)
                            .isEqualTo(firstResult.winner().get().criteriaName());
                }
            }
        }
    }
}
