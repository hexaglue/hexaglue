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

package io.hexaglue.arch;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.syntax.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationTrace")
class ClassificationTraceTest {

    private static final ElementKind AGGREGATE_ROOT = ElementKind.AGGREGATE_ROOT;
    private static final ConfidenceLevel HIGH = ConfidenceLevel.HIGH;
    private static final ConfidenceLevel LOW = ConfidenceLevel.LOW;

    @Nested
    @DisplayName("ConfidenceLevel")
    class ConfidenceLevelTest {

        @Test
        @DisplayName("should have HIGH, MEDIUM, and LOW levels")
        void shouldHaveAllLevels() {
            assertThat(ConfidenceLevel.values())
                    .containsExactly(ConfidenceLevel.HIGH, ConfidenceLevel.MEDIUM, ConfidenceLevel.LOW);
        }

        @Test
        @DisplayName("HIGH should be greater than MEDIUM")
        void highShouldBeGreaterThanMedium() {
            assertThat(ConfidenceLevel.HIGH.compareTo(ConfidenceLevel.MEDIUM)).isLessThan(0);
        }

        @Test
        @DisplayName("MEDIUM should be greater than LOW")
        void mediumShouldBeGreaterThanLow() {
            assertThat(ConfidenceLevel.MEDIUM.compareTo(ConfidenceLevel.LOW)).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("EvidenceType")
    class EvidenceTypeTest {

        @Test
        @DisplayName("should have all expected evidence types")
        void shouldHaveAllTypes() {
            assertThat(EvidenceType.values())
                    .containsExactly(
                            EvidenceType.ANNOTATION,
                            EvidenceType.NAMING,
                            EvidenceType.STRUCTURE,
                            EvidenceType.RELATIONSHIP,
                            EvidenceType.PACKAGE,
                            EvidenceType.BEHAVIOR);
        }
    }

    @Nested
    @DisplayName("Evidence")
    class EvidenceTest {

        @Test
        @DisplayName("should create evidence with all fields")
        void shouldCreateWithAllFields() {
            // given
            SourceLocation location = SourceLocation.at(Path.of("Order.java"), 10, 5);

            // when
            Evidence evidence =
                    new Evidence(EvidenceType.ANNOTATION, "@AggregateRoot annotation found", Optional.of(location));

            // then
            assertThat(evidence.type()).isEqualTo(EvidenceType.ANNOTATION);
            assertThat(evidence.description()).isEqualTo("@AggregateRoot annotation found");
            assertThat(evidence.location()).contains(location);
        }

        @Test
        @DisplayName("should create evidence without location")
        void shouldCreateWithoutLocation() {
            // when
            Evidence evidence = new Evidence(EvidenceType.NAMING, "Name ends with Repository", Optional.empty());

            // then
            assertThat(evidence.type()).isEqualTo(EvidenceType.NAMING);
            assertThat(evidence.location()).isEmpty();
        }
    }

    @Nested
    @DisplayName("AppliedCriterion")
    class AppliedCriterionTest {

        @Test
        @DisplayName("should create matched criterion")
        void shouldCreateMatchedCriterion() {
            // given
            Evidence evidence = new Evidence(EvidenceType.ANNOTATION, "@AggregateRoot found", Optional.empty());

            // when
            AppliedCriterion criterion = new AppliedCriterion(
                    "explicit-aggregate-root",
                    100,
                    true,
                    AGGREGATE_ROOT,
                    "Type has @AggregateRoot annotation",
                    List.of(evidence));

            // then
            assertThat(criterion.name()).isEqualTo("explicit-aggregate-root");
            assertThat(criterion.priority()).isEqualTo(100);
            assertThat(criterion.matched()).isTrue();
            assertThat(criterion.suggestedKind()).isEqualTo(AGGREGATE_ROOT);
            assertThat(criterion.explanation()).isEqualTo("Type has @AggregateRoot annotation");
            assertThat(criterion.evidence()).hasSize(1);
        }

        @Test
        @DisplayName("should create unmatched criterion")
        void shouldCreateUnmatchedCriterion() {
            // when
            AppliedCriterion criterion = new AppliedCriterion(
                    "jmolecules-entity", 100, false, ElementKind.ENTITY, "Type has @Entity annotation", List.of());

            // then
            assertThat(criterion.matched()).isFalse();
            assertThat(criterion.evidence()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ConflictInfo")
    class ConflictInfoTest {

        @Test
        @DisplayName("should create conflict info")
        void shouldCreateConflictInfo() {
            // when
            ConflictInfo conflict =
                    new ConflictInfo(ElementKind.ENTITY, "Also has identity field", ConfidenceLevel.MEDIUM);

            // then
            assertThat(conflict.alternativeKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(conflict.reason()).isEqualTo("Also has identity field");
            assertThat(conflict.alternativeConfidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("SemanticContext")
    class SemanticContextTest {

        @Test
        @DisplayName("should create semantic context with all values")
        void shouldCreateWithAllValues() {
            // when
            SemanticContext context = new SemanticContext(
                    Optional.of("DOMAIN_ANCHOR"),
                    Optional.of("PIVOT"),
                    List.of("Serializable", "Comparable"),
                    List.of("OrderRepository"));

            // then
            assertThat(context.anchorKind()).contains("DOMAIN_ANCHOR");
            assertThat(context.coreAppClassRole()).contains("PIVOT");
            assertThat(context.implementedInterfaces()).containsExactly("Serializable", "Comparable");
            assertThat(context.usedInterfaces()).containsExactly("OrderRepository");
        }

        @Test
        @DisplayName("should create empty semantic context")
        void shouldCreateEmpty() {
            // when
            SemanticContext context = SemanticContext.empty();

            // then
            assertThat(context.anchorKind()).isEmpty();
            assertThat(context.coreAppClassRole()).isEmpty();
            assertThat(context.implementedInterfaces()).isEmpty();
            assertThat(context.usedInterfaces()).isEmpty();
        }
    }

    @Nested
    @DisplayName("RemediationAction")
    class RemediationActionTest {

        @Test
        @DisplayName("should have all expected action types")
        void shouldHaveAllActionTypes() {
            assertThat(RemediationAction.values())
                    .containsExactly(
                            RemediationAction.ADD_ANNOTATION,
                            RemediationAction.CONFIGURE_EXPLICIT,
                            RemediationAction.RENAME,
                            RemediationAction.MOVE_PACKAGE,
                            RemediationAction.IMPLEMENT_INTERFACE,
                            RemediationAction.EXCLUDE);
        }
    }

    @Nested
    @DisplayName("RemediationImpact")
    class RemediationImpactTest {

        @Test
        @DisplayName("should create explicit impact")
        void shouldCreateExplicitImpact() {
            // when
            RemediationImpact impact = RemediationImpact.explicit(AGGREGATE_ROOT);

            // then
            assertThat(impact.resultingKind()).isEqualTo(AGGREGATE_ROOT);
            assertThat(impact.resultingConfidence()).isEqualTo(HIGH);
            assertThat(impact.description()).contains("AGGREGATE_ROOT").contains("HIGH");
        }

        @Test
        @DisplayName("should create improved impact")
        void shouldCreateImprovedImpact() {
            // when
            RemediationImpact impact = RemediationImpact.improved(ElementKind.ENTITY, ConfidenceLevel.MEDIUM);

            // then
            assertThat(impact.resultingKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(impact.resultingConfidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(impact.description()).contains("MEDIUM");
        }
    }

    @Nested
    @DisplayName("RemediationHint")
    class RemediationHintTest {

        @Test
        @DisplayName("should create hint with code snippet")
        void shouldCreateWithCodeSnippet() {
            // given
            RemediationImpact impact = RemediationImpact.explicit(AGGREGATE_ROOT);

            // when
            RemediationHint hint = new RemediationHint(
                    RemediationAction.ADD_ANNOTATION,
                    "Add @AggregateRoot annotation",
                    impact,
                    Optional.of("@AggregateRoot\npublic class Order { }"));

            // then
            assertThat(hint.actionType()).isEqualTo(RemediationAction.ADD_ANNOTATION);
            assertThat(hint.description()).isEqualTo("Add @AggregateRoot annotation");
            assertThat(hint.impact()).isEqualTo(impact);
            assertThat(hint.codeSnippet()).isPresent();
            assertThat(hint.codeSnippet().orElseThrow()).contains("@AggregateRoot");
        }

        @Test
        @DisplayName("should provide action description shortcut")
        void shouldProvideActionShortcut() {
            // given
            RemediationHint hint = new RemediationHint(
                    RemediationAction.CONFIGURE_EXPLICIT,
                    "Configure in hexaglue.yaml",
                    RemediationImpact.explicit(AGGREGATE_ROOT),
                    Optional.empty());

            // then
            assertThat(hint.action()).isEqualTo("Configure in hexaglue.yaml");
        }
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTest {

        @Test
        @DisplayName("should create classification trace with all fields")
        void shouldCreateWithAllFields() {
            // given
            AppliedCriterion winning = new AppliedCriterion(
                    "explicit-aggregate-root", 100, true, AGGREGATE_ROOT, "Has annotation", List.of());
            ConflictInfo conflict = new ConflictInfo(ElementKind.ENTITY, "Also has ID", ConfidenceLevel.MEDIUM);
            RemediationHint hint = new RemediationHint(
                    RemediationAction.ADD_ANNOTATION,
                    "Add annotation",
                    RemediationImpact.explicit(AGGREGATE_ROOT),
                    Optional.empty());

            // when
            ClassificationTrace trace = new ClassificationTrace(
                    AGGREGATE_ROOT,
                    HIGH,
                    winning,
                    List.of(winning),
                    List.of(conflict),
                    SemanticContext.empty(),
                    List.of(hint));

            // then
            assertThat(trace.classifiedAs()).isEqualTo(AGGREGATE_ROOT);
            assertThat(trace.confidence()).isEqualTo(HIGH);
            assertThat(trace.winningCriterion()).isEqualTo(winning);
            assertThat(trace.evaluatedCriteria()).hasSize(1);
            assertThat(trace.conflicts()).hasSize(1);
            assertThat(trace.remediationHints()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Explain")
    class ExplainTest {

        @Test
        @DisplayName("should explain classification with winning criterion")
        void shouldExplainWithWinningCriterion() {
            // given
            ClassificationTrace trace = createHighConfidenceTrace();

            // when
            String explanation = trace.explain();

            // then
            assertThat(explanation).contains("AGGREGATE_ROOT");
            assertThat(explanation).contains("HIGH");
            assertThat(explanation).contains("Has @AggregateRoot annotation");
        }

        @Test
        @DisplayName("should explain with conflicts")
        void shouldExplainWithConflicts() {
            // given
            ClassificationTrace trace = createTraceWithConflict();

            // when
            String explanation = trace.explain();

            // then
            assertThat(explanation).contains("Conflicts");
            assertThat(explanation).contains("ENTITY");
            assertThat(explanation).contains("Also has identity field");
        }

        @Test
        @DisplayName("should explain with remediation hints")
        void shouldExplainWithRemediationHints() {
            // given
            ClassificationTrace trace = createTraceWithRemediationHints();

            // when
            String explanation = trace.explain();

            // then
            assertThat(explanation).contains("explicit");
            assertThat(explanation).contains("Add @AggregateRoot");
        }

        @Test
        @DisplayName("should provide brief explanation")
        void shouldProvideBriefExplanation() {
            // given
            ClassificationTrace trace = createHighConfidenceTrace();

            // when
            String brief = trace.explainBrief();

            // then
            assertThat(brief).contains("AGGREGATE_ROOT");
            assertThat(brief).contains("HIGH");
            assertThat(brief).doesNotContain("\n");
        }
    }

    @Nested
    @DisplayName("NeedsClarification")
    class NeedsClarificationTest {

        @Test
        @DisplayName("should not need clarification for high confidence")
        void shouldNotNeedClarificationForHighConfidence() {
            // given
            ClassificationTrace trace = createHighConfidenceTrace();

            // then
            assertThat(trace.needsClarification()).isFalse();
        }

        @Test
        @DisplayName("should need clarification for low confidence")
        void shouldNeedClarificationForLowConfidence() {
            // given
            ClassificationTrace trace = createLowConfidenceTrace();

            // then
            assertThat(trace.needsClarification()).isTrue();
        }

        @Test
        @DisplayName("should need clarification for UNCLASSIFIED")
        void shouldNeedClarificationForUnclassified() {
            // given
            ClassificationTrace trace = createUnclassifiedTrace();

            // then
            assertThat(trace.needsClarification()).isTrue();
        }

        @Test
        @DisplayName("should need clarification when conflicts exist")
        void shouldNeedClarificationWithConflicts() {
            // given
            ClassificationTrace trace = createTraceWithConflict();

            // then
            assertThat(trace.needsClarification()).isTrue();
        }
    }

    @Nested
    @DisplayName("FactoryMethods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("should create high confidence trace via factory")
        void shouldCreateHighConfidenceViaFactory() {
            // when
            ClassificationTrace trace = ClassificationTrace.highConfidence(
                    AGGREGATE_ROOT, "explicit-annotation", "Has @AggregateRoot annotation");

            // then
            assertThat(trace.classifiedAs()).isEqualTo(AGGREGATE_ROOT);
            assertThat(trace.confidence()).isEqualTo(HIGH);
            assertThat(trace.winningCriterion().name()).isEqualTo("explicit-annotation");
            assertThat(trace.needsClarification()).isFalse();
        }

        @Test
        @DisplayName("should create unclassified trace via factory")
        void shouldCreateUnclassifiedViaFactory() {
            // when
            ClassificationTrace trace = ClassificationTrace.unclassified("No matching criterion", List.of());

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(trace.confidence()).isEqualTo(LOW);
            assertThat(trace.needsClarification()).isTrue();
        }
    }

    // === Test Helpers ===

    private ClassificationTrace createHighConfidenceTrace() {
        AppliedCriterion winning = new AppliedCriterion(
                "explicit-annotation", 100, true, AGGREGATE_ROOT, "Has @AggregateRoot annotation", List.of());
        return new ClassificationTrace(
                AGGREGATE_ROOT, HIGH, winning, List.of(winning), List.of(), SemanticContext.empty(), List.of());
    }

    private ClassificationTrace createLowConfidenceTrace() {
        AppliedCriterion winning = new AppliedCriterion(
                "naming-convention", 60, true, ElementKind.VALUE_OBJECT, "Name suggests value", List.of());
        return new ClassificationTrace(
                ElementKind.VALUE_OBJECT,
                LOW,
                winning,
                List.of(winning),
                List.of(),
                SemanticContext.empty(),
                List.of());
    }

    private ClassificationTrace createUnclassifiedTrace() {
        AppliedCriterion notMatched =
                new AppliedCriterion("no-criterion", 0, false, ElementKind.UNCLASSIFIED, "No match", List.of());
        return new ClassificationTrace(
                ElementKind.UNCLASSIFIED, LOW, notMatched, List.of(), List.of(), SemanticContext.empty(), List.of());
    }

    private ClassificationTrace createTraceWithConflict() {
        AppliedCriterion winning = new AppliedCriterion(
                "repository-primary-type", 85, true, AGGREGATE_ROOT, "Used in repository", List.of());
        ConflictInfo conflict = new ConflictInfo(ElementKind.ENTITY, "Also has identity field", ConfidenceLevel.MEDIUM);
        return new ClassificationTrace(
                AGGREGATE_ROOT, HIGH, winning, List.of(winning), List.of(conflict), SemanticContext.empty(), List.of());
    }

    private ClassificationTrace createTraceWithRemediationHints() {
        AppliedCriterion winning =
                new AppliedCriterion("heuristic-match", 70, true, AGGREGATE_ROOT, "Looks like aggregate", List.of());
        RemediationHint hint = new RemediationHint(
                RemediationAction.ADD_ANNOTATION,
                "Add @AggregateRoot annotation",
                RemediationImpact.explicit(AGGREGATE_ROOT),
                Optional.empty());
        return new ClassificationTrace(
                AGGREGATE_ROOT,
                ConfidenceLevel.MEDIUM,
                winning,
                List.of(winning),
                List.of(),
                SemanticContext.empty(),
                List.of(hint));
    }
}
