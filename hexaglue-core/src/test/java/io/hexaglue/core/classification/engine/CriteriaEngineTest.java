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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
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
    private CompatibilityPolicy<ElementKind> compatibilityPolicy;

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

    private CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> createEngine(
            List<ClassificationCriteria<ElementKind>> criteria) {
        return new CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>>(
                criteria, new DefaultDecisionPolicy<>(), compatibilityPolicy, this::buildContribution);
    }

    private Contribution<ElementKind> buildContribution(
            ClassificationCriteria<ElementKind> criteria, MatchResult result, int priority) {
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

    private ClassificationCriteria<ElementKind> createCriteria(
            String name, int priority, ElementKind targetKind, NodeEvaluator evaluator) {

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
            public ElementKind targetKind() {
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
            assertThatThrownBy(() -> new CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>>(
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
            assertThatThrownBy(() -> new CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>>(
                            List.of(), new DefaultDecisionPolicy<>(), null, CriteriaEngineTest.this::buildContribution))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("compatibilityPolicy");
        }

        @Test
        @DisplayName("should reject null contribution builder")
        void shouldRejectNullContributionBuilder() {
            assertThatThrownBy(() -> new CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>>(
                            List.of(), new DefaultDecisionPolicy<>(), compatibilityPolicy, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("contributionBuilder");
        }

        @Test
        @DisplayName("should create engine with empty criteria list")
        void shouldCreateWithEmptyCriteria() {
            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine = createEngine(List.of());

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
            ClassificationCriteria<ElementKind> nonMatchingCriteria =
                    createCriteria("non-matching", 80, ElementKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine =
                    createEngine(List.of(nonMatchingCriteria));

            List<Contribution<ElementKind>> contributions = engine.evaluate(testNode, mockQuery);

            assertThat(contributions).isEmpty();
        }

        @Test
        @DisplayName("should return contributions from matching criteria")
        void shouldReturnContributionsFromMatchingCriteria() {
            ClassificationCriteria<ElementKind> matching = createCriteria(
                    "matching",
                    80,
                    ElementKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.HIGH, "Has repository"));

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine = createEngine(List.of(matching));

            List<Contribution<ElementKind>> contributions = engine.evaluate(testNode, mockQuery);

            assertThat(contributions).hasSize(1);
            assertThat(contributions.get(0).kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(contributions.get(0).criteriaName()).isEqualTo("matching");
        }

        @Test
        @DisplayName("should evaluate all criteria and collect matching ones")
        void shouldEvaluateAllCriteria() {
            ClassificationCriteria<ElementKind> match1 = createCriteria(
                    "match1",
                    100,
                    ElementKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "A"));
            ClassificationCriteria<ElementKind> noMatch =
                    createCriteria("noMatch", 90, ElementKind.ENTITY, node -> MatchResult.noMatch());
            ClassificationCriteria<ElementKind> match2 = createCriteria(
                    "match2", 80, ElementKind.VALUE_OBJECT, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine =
                    createEngine(List.of(match1, noMatch, match2));

            List<Contribution<ElementKind>> contributions = engine.evaluate(testNode, mockQuery);

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
            ClassificationCriteria<ElementKind> noMatch =
                    createCriteria("noMatch", 80, ElementKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine = createEngine(List.of(noMatch));

            DecisionPolicy.Decision<ElementKind> decision = engine.classify(testNode, mockQuery);

            assertThat(decision.isEmpty()).isTrue();
            assertThat(decision.hasWinner()).isFalse();
        }

        @Test
        @DisplayName("should return winner based on priority")
        void shouldReturnWinnerBasedOnPriority() {
            ClassificationCriteria<ElementKind> highPriority = createCriteria(
                    "highPriority",
                    100,
                    ElementKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.LOW, "A"));
            ClassificationCriteria<ElementKind> lowPriority = createCriteria(
                    "lowPriority", 50, ElementKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "B"));

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine =
                    createEngine(List.of(lowPriority, highPriority));

            DecisionPolicy.Decision<ElementKind> decision = engine.classify(testNode, mockQuery);

            assertThat(decision.hasWinner()).isTrue();
            assertThat(decision.winner().get().kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(decision.winner().get().priority()).isEqualTo(100);
        }

        @Test
        @DisplayName("should detect conflicts with different kinds")
        void shouldDetectConflicts() {
            ClassificationCriteria<ElementKind> criteria1 = createCriteria(
                    "criteria1",
                    100,
                    ElementKind.AGGREGATE_ROOT,
                    node -> MatchResult.match(ConfidenceLevel.EXPLICIT, "A"));
            ClassificationCriteria<ElementKind> criteria2 = createCriteria(
                    "criteria2", 80, ElementKind.VALUE_OBJECT, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine =
                    createEngine(List.of(criteria1, criteria2));

            DecisionPolicy.Decision<ElementKind> decision = engine.classify(testNode, mockQuery);

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
            ClassificationCriteria<ElementKind> criteria =
                    createCriteria("test", 80, ElementKind.ENTITY, node -> MatchResult.noMatch());

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine = createEngine(List.of(criteria));

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
            ClassificationCriteria<ElementKind> criteria1 = createCriteria(
                    "criteria1", 80, ElementKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.HIGH, "A"));
            ClassificationCriteria<ElementKind> criteria2 = createCriteria(
                    "criteria2", 80, ElementKind.ENTITY, node -> MatchResult.match(ConfidenceLevel.HIGH, "B"));

            CriteriaEngine<ElementKind, ClassificationCriteria<ElementKind>> engine =
                    createEngine(List.of(criteria1, criteria2));

            DecisionPolicy.Decision<ElementKind> firstResult = engine.classify(testNode, mockQuery);

            for (int i = 0; i < 100; i++) {
                DecisionPolicy.Decision<ElementKind> result = engine.classify(testNode, mockQuery);

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
