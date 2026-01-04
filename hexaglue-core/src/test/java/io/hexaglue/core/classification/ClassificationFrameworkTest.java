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

package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the classification framework types.
 */
class ClassificationFrameworkTest {

    // =========================================================================
    // ConfidenceLevel
    // =========================================================================

    @Nested
    class ConfidenceLevelTest {

        @Test
        void shouldHaveCorrectOrdering() {
            assertThat(ConfidenceLevel.EXPLICIT.weight()).isGreaterThan(ConfidenceLevel.HIGH.weight());
            assertThat(ConfidenceLevel.HIGH.weight()).isGreaterThan(ConfidenceLevel.MEDIUM.weight());
            assertThat(ConfidenceLevel.MEDIUM.weight()).isGreaterThan(ConfidenceLevel.LOW.weight());
        }

        @Test
        void shouldCompareWithIsAtLeast() {
            assertThat(ConfidenceLevel.EXPLICIT.isAtLeast(ConfidenceLevel.HIGH)).isTrue();
            assertThat(ConfidenceLevel.HIGH.isAtLeast(ConfidenceLevel.HIGH)).isTrue();
            assertThat(ConfidenceLevel.MEDIUM.isAtLeast(ConfidenceLevel.HIGH)).isFalse();
        }

        @Test
        void shouldCompareWithIsHigherThan() {
            assertThat(ConfidenceLevel.EXPLICIT.isHigherThan(ConfidenceLevel.HIGH))
                    .isTrue();
            assertThat(ConfidenceLevel.HIGH.isHigherThan(ConfidenceLevel.HIGH)).isFalse();
            assertThat(ConfidenceLevel.MEDIUM.isHigherThan(ConfidenceLevel.HIGH))
                    .isFalse();
        }
    }

    // =========================================================================
    // Evidence
    // =========================================================================

    @Nested
    class EvidenceTest {

        @Test
        void shouldCreateFromAnnotation() {
            NodeId nodeId = NodeId.type("com.example.Order");

            Evidence evidence = Evidence.fromAnnotation("AggregateRoot", nodeId);

            assertThat(evidence.type()).isEqualTo(EvidenceType.ANNOTATION);
            assertThat(evidence.description()).contains("@AggregateRoot");
            assertThat(evidence.relatedNodes()).containsExactly(nodeId);
        }

        @Test
        void shouldCreateFromNaming() {
            Evidence evidence = Evidence.fromNaming("*Repository", "OrderRepository");

            assertThat(evidence.type()).isEqualTo(EvidenceType.NAMING);
            assertThat(evidence.description()).contains("OrderRepository");
            assertThat(evidence.description()).contains("*Repository");
        }

        @Test
        void shouldCreateFromStructure() {
            NodeId fieldId = NodeId.field("com.example.Order", "id");

            Evidence evidence = Evidence.fromStructure("Has identity field 'id'", List.of(fieldId));

            assertThat(evidence.type()).isEqualTo(EvidenceType.STRUCTURE);
            assertThat(evidence.relatedNodes()).containsExactly(fieldId);
        }

        @Test
        void shouldCreateFromRelationship() {
            NodeId repoId = NodeId.type("com.example.OrderRepository");

            Evidence evidence = Evidence.fromRelationship("Used in repository signature", List.of(repoId));

            assertThat(evidence.type()).isEqualTo(EvidenceType.RELATIONSHIP);
            assertThat(evidence.relatedNodes()).containsExactly(repoId);
        }

        @Test
        void shouldCreateFromPackage() {
            Evidence evidence = Evidence.fromPackage("com.example.ports.in", "indicates driving port");

            assertThat(evidence.type()).isEqualTo(EvidenceType.PACKAGE);
            assertThat(evidence.description()).contains("com.example.ports.in");
        }

        @Test
        void shouldRejectNullType() {
            assertThatThrownBy(() -> new Evidence(null, "desc", List.of())).isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectNullDescription() {
            assertThatThrownBy(() -> new Evidence(EvidenceType.NAMING, null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldDefaultToEmptyRelatedNodes() {
            Evidence evidence = new Evidence(EvidenceType.NAMING, "test", null);

            assertThat(evidence.relatedNodes()).isEmpty();
        }
    }

    // =========================================================================
    // MatchResult
    // =========================================================================

    @Nested
    class MatchResultTest {

        @Test
        void shouldCreateNoMatch() {
            MatchResult result = MatchResult.noMatch();

            assertThat(result.matched()).isFalse();
            assertThat(result.isNoMatch()).isTrue();
            assertThat(result.confidence()).isNull();
            assertThat(result.justification()).isNull();
            assertThat(result.evidence()).isEmpty();
        }

        @Test
        void shouldCreateMatch() {
            MatchResult result = MatchResult.match(ConfidenceLevel.HIGH, "Has identity field");

            assertThat(result.matched()).isTrue();
            assertThat(result.isNoMatch()).isFalse();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).isEqualTo("Has identity field");
        }

        @Test
        void shouldCreateMatchWithEvidence() {
            Evidence evidence = Evidence.fromNaming("*Id", "OrderId");

            MatchResult result = MatchResult.match(ConfidenceLevel.MEDIUM, "Name matches pattern", evidence);

            assertThat(result.evidence()).hasSize(1);
            assertThat(result.evidence().get(0)).isEqualTo(evidence);
        }

        @Test
        void shouldCreateExplicitAnnotationMatch() {
            NodeId nodeId = NodeId.type("com.example.Order");

            MatchResult result = MatchResult.explicitAnnotation("AggregateRoot", nodeId);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("@AggregateRoot");
            assertThat(result.evidence()).hasSize(1);
            assertThat(result.evidence().get(0).type()).isEqualTo(EvidenceType.ANNOTATION);
        }

        @Test
        void shouldCheckMatchedWithAtLeast() {
            MatchResult highMatch = MatchResult.match(ConfidenceLevel.HIGH, "test");
            MatchResult mediumMatch = MatchResult.match(ConfidenceLevel.MEDIUM, "test");
            MatchResult noMatch = MatchResult.noMatch();

            assertThat(highMatch.matchedWithAtLeast(ConfidenceLevel.HIGH)).isTrue();
            assertThat(highMatch.matchedWithAtLeast(ConfidenceLevel.EXPLICIT)).isFalse();
            assertThat(mediumMatch.matchedWithAtLeast(ConfidenceLevel.HIGH)).isFalse();
            assertThat(noMatch.matchedWithAtLeast(ConfidenceLevel.LOW)).isFalse();
        }

        @Test
        void shouldRejectMatchWithoutConfidence() {
            assertThatThrownBy(() -> new MatchResult(true, null, "justification", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("confidence required");
        }

        @Test
        void shouldRejectMatchWithoutJustification() {
            assertThatThrownBy(() -> new MatchResult(true, ConfidenceLevel.HIGH, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("justification required");
        }
    }

    // =========================================================================
    // ClassificationCriteria
    // =========================================================================

    @Nested
    class ClassificationCriteriaTest {

        enum TestKind {
            A,
            B
        }

        @Test
        void shouldImplementCriteriaInterface() {
            ClassificationCriteria<TestKind> criteria = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "test-criteria";
                }

                @Override
                public int priority() {
                    return 80;
                }

                @Override
                public TestKind targetKind() {
                    return TestKind.A;
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    if (node.simpleName().endsWith("Test")) {
                        return MatchResult.match(ConfidenceLevel.HIGH, "Name ends with Test");
                    }
                    return MatchResult.noMatch();
                }
            };

            assertThat(criteria.name()).isEqualTo("test-criteria");
            assertThat(criteria.priority()).isEqualTo(80);
            assertThat(criteria.targetKind()).isEqualTo(TestKind.A);
            assertThat(criteria.description()).contains("test-criteria");
            assertThat(criteria.description()).contains("A");
        }
    }

    // =========================================================================
    // ClassificationResult
    // =========================================================================

    @Nested
    class ClassificationResultTest {

        @Test
        void shouldCreateClassifiedResult() {
            NodeId nodeId = NodeId.type("com.example.Order");
            Evidence evidence = Evidence.fromAnnotation("AggregateRoot", nodeId);

            ClassificationResult result = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.EXPLICIT,
                    "explicit-annotation",
                    100,
                    "Annotated with @AggregateRoot",
                    List.of(evidence),
                    List.of());

            assertThat(result.isClassified()).isTrue();
            assertThat(result.isUnclassified()).isFalse();
            assertThat(result.status()).isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.target()).isEqualTo(ClassificationTarget.DOMAIN);
            assertThat(result.hasConflicts()).isFalse();
        }

        @Test
        void shouldCreateUnclassifiedResult() {
            NodeId nodeId = NodeId.type("com.example.Unknown");

            ClassificationResult result = ClassificationResult.unclassified(nodeId);

            assertThat(result.isClassified()).isFalse();
            assertThat(result.isUnclassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.UNCLASSIFIED);
            assertThat(result.kind()).isNull();
            assertThat(result.kindOpt()).isEmpty();
        }

        @Test
        void shouldCreateConflictResult() {
            NodeId nodeId = NodeId.type("com.example.Ambiguous");
            Conflict conflict = Conflict.between("ENTITY", "has-identity", ConfidenceLevel.HIGH, 70, "VALUE_OBJECT");

            ClassificationResult result = ClassificationResult.conflict(nodeId, List.of(conflict));

            assertThat(result.isClassified()).isFalse();
            assertThat(result.status()).isEqualTo(ClassificationStatus.CONFLICT);
            assertThat(result.hasConflicts()).isTrue();
            assertThat(result.conflicts()).hasSize(1);
        }

        @Test
        void shouldReturnOptionals() {
            NodeId nodeId = NodeId.type("com.example.Order");

            ClassificationResult classified = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "ENTITY",
                    ConfidenceLevel.HIGH,
                    "test",
                    80,
                    "test",
                    List.of(),
                    List.of());

            ClassificationResult unclassified = ClassificationResult.unclassified(nodeId);

            assertThat(classified.kindOpt()).contains("ENTITY");
            assertThat(classified.confidenceOpt()).contains(ConfidenceLevel.HIGH);

            assertThat(unclassified.kindOpt()).isEmpty();
            assertThat(unclassified.confidenceOpt()).isEmpty();
        }
    }

    // =========================================================================
    // Conflict
    // =========================================================================

    @Nested
    class ConflictTest {

        @Test
        void shouldCreateConflict() {
            Conflict conflict = new Conflict(
                    "ENTITY", "has-identity", ConfidenceLevel.HIGH, 70, "Matched as ENTITY but winner is VALUE_OBJECT");

            assertThat(conflict.competingKind()).isEqualTo("ENTITY");
            assertThat(conflict.competingCriteria()).isEqualTo("has-identity");
            assertThat(conflict.competingConfidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(conflict.competingPriority()).isEqualTo(70);
        }

        @Test
        void shouldCreateConflictBetween() {
            Conflict conflict = Conflict.between("ENTITY", "has-identity", ConfidenceLevel.HIGH, 70, "VALUE_OBJECT");

            assertThat(conflict.competingKind()).isEqualTo("ENTITY");
            assertThat(conflict.competingPriority()).isEqualTo(70);
            assertThat(conflict.rationale()).contains("ENTITY");
            assertThat(conflict.rationale()).contains("VALUE_OBJECT");
        }

        @Test
        void shouldRejectNullFields() {
            assertThatThrownBy(() -> new Conflict(null, "test", ConfidenceLevel.HIGH, 80, "reason"))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new Conflict("kind", null, ConfidenceLevel.HIGH, 80, "reason"))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new Conflict("kind", "test", null, 80, "reason"))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new Conflict("kind", "test", ConfidenceLevel.HIGH, 80, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
