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

package io.hexaglue.arch.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.Evidence;
import io.hexaglue.arch.EvidenceType;
import io.hexaglue.syntax.AnnotationSyntax;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DomainClassifier")
class DomainClassifierTest {

    private ClassificationContext context;

    @BeforeEach
    void setUp() {
        context = ClassificationContext.empty(new StubSyntaxProvider());
    }

    @Nested
    @DisplayName("Explicit Annotation Classification")
    class ExplicitAnnotationTest {

        @Test
        @DisplayName("should classify as AGGREGATE_ROOT when @AggregateRoot annotation present")
        void shouldClassifyAsAggregateRootWithAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order").withAnnotation("io.hexaglue.ddd.AggregateRoot");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitAggregateRootCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(trace.winningCriterion().name()).isEqualTo("explicit-aggregate-root");
        }

        @Test
        @DisplayName("should classify as VALUE_OBJECT when @ValueObject annotation present")
        void shouldClassifyAsValueObjectWithAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money").withAnnotation("io.hexaglue.ddd.ValueObject");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitValueObjectCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should classify as ENTITY when @Entity annotation present")
        void shouldClassifyAsEntityWithAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.OrderLine").withAnnotation("io.hexaglue.ddd.Entity");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitEntityCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.ENTITY);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should classify as IDENTIFIER when @Identifier annotation present")
        void shouldClassifyAsIdentifierWithAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.OrderId").withAnnotation("io.hexaglue.ddd.Identifier");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitIdentifierCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.IDENTIFIER);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should classify as DOMAIN_EVENT when @DomainEvent annotation present")
        void shouldClassifyAsDomainEventWithAnnotation() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderPlaced").withAnnotation("io.hexaglue.ddd.DomainEvent");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitDomainEventCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DOMAIN_EVENT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("Priority Resolution")
    class PriorityResolutionTest {

        @Test
        @DisplayName("should prefer higher priority criterion when multiple match")
        void shouldPreferHigherPriority() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money")
                    .withAnnotation("io.hexaglue.ddd.ValueObject")
                    .asRecord();
            DomainClassifier classifier = new DomainClassifier(List.of(
                    new ExplicitValueObjectCriterion(), // priority 100
                    new RecordValueObjectCriterion() // priority 70
                    ));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(trace.winningCriterion().name()).isEqualTo("explicit-value-object");
            assertThat(trace.winningCriterion().priority()).isEqualTo(100);
        }

        @Test
        @DisplayName("should prefer explicit annotation over heuristic")
        void shouldPreferExplicitOverHeuristic() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order").withAnnotation("io.hexaglue.ddd.AggregateRoot");
            ClassificationContext ctxWithRepository = ClassificationContext.builder(new StubSyntaxProvider())
                    .repositoryDominantTypes(Set.of("com.example.Order"))
                    .build();
            DomainClassifier classifier = new DomainClassifier(List.of(
                    new ExplicitAggregateRootCriterion(), // priority 100
                    new RepositoryDominantCriterion() // priority 80
                    ));

            // when
            ClassificationTrace trace = classifier.classify(type, ctxWithRepository);

            // then
            assertThat(trace.winningCriterion().name()).isEqualTo("explicit-aggregate-root");
        }
    }

    @Nested
    @DisplayName("Heuristic Classification")
    class HeuristicClassificationTest {

        @Test
        @DisplayName("should classify as AGGREGATE_ROOT when used as repository dominant type")
        void shouldClassifyAsAggregateRootFromRepository() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order");
            ClassificationContext ctxWithRepository = ClassificationContext.builder(new StubSyntaxProvider())
                    .repositoryDominantTypes(Set.of("com.example.Order"))
                    .build();
            DomainClassifier classifier = new DomainClassifier(List.of(new RepositoryDominantCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, ctxWithRepository);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(trace.winningCriterion().name()).isEqualTo("repository-dominant");
        }

        @Test
        @DisplayName("should classify record as VALUE_OBJECT heuristically")
        void shouldClassifyRecordAsValueObject() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money").asRecord();
            DomainClassifier classifier = new DomainClassifier(List.of(new RecordValueObjectCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Unclassified")
    class UnclassifiedTest {

        @Test
        @DisplayName("should return UNCLASSIFIED when no criteria match")
        void shouldReturnUnclassifiedWhenNoCriteriaMatch() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.SomeUtility");
            DomainClassifier classifier = new DomainClassifier(
                    List.of(new ExplicitAggregateRootCriterion(), new ExplicitValueObjectCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.LOW);
            assertThat(trace.needsClarification()).isTrue();
        }

        @Test
        @DisplayName("should return UNCLASSIFIED with empty criteria list")
        void shouldReturnUnclassifiedWithEmptyCriteria() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order");
            DomainClassifier classifier = new DomainClassifier(List.of());

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
        }
    }

    @Nested
    @DisplayName("Trace Information")
    class TraceInformationTest {

        @Test
        @DisplayName("should include all evaluated criteria in trace")
        void shouldIncludeAllEvaluatedCriteria() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order").withAnnotation("io.hexaglue.ddd.AggregateRoot");
            DomainClassifier classifier = new DomainClassifier(List.of(
                    new ExplicitAggregateRootCriterion(),
                    new ExplicitValueObjectCriterion(),
                    new ExplicitEntityCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.evaluatedCriteria()).hasSize(3);
            assertThat(trace.evaluatedCriteria().stream()
                            .filter(c -> c.matched())
                            .count())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should include evidence in winning criterion")
        void shouldIncludeEvidenceInWinningCriterion() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order").withAnnotation("io.hexaglue.ddd.AggregateRoot");
            DomainClassifier classifier = new DomainClassifier(List.of(new ExplicitAggregateRootCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.winningCriterion().evidence()).isNotEmpty();
            assertThat(trace.winningCriterion().evidence().get(0).type()).isEqualTo(EvidenceType.ANNOTATION);
        }
    }

    // ===== Test Criteria =====

    /** Test criterion for explicit @AggregateRoot annotation. */
    private static class ExplicitAggregateRootCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "explicit-aggregate-root";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.AGGREGATE_ROOT;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            return type.annotations().stream()
                    .filter(a -> a.qualifiedName().endsWith("AggregateRoot"))
                    .findFirst()
                    .map(a -> CriterionMatch.high(
                            "Type has @AggregateRoot annotation",
                            Evidence.of(EvidenceType.ANNOTATION, "@AggregateRoot annotation found")));
        }
    }

    /** Test criterion for explicit @ValueObject annotation. */
    private static class ExplicitValueObjectCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "explicit-value-object";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.VALUE_OBJECT;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            return type.annotations().stream()
                    .filter(a -> a.qualifiedName().endsWith("ValueObject"))
                    .findFirst()
                    .map(a -> CriterionMatch.high(
                            "Type has @ValueObject annotation",
                            Evidence.of(EvidenceType.ANNOTATION, "@ValueObject annotation found")));
        }
    }

    /** Test criterion for explicit @Entity annotation. */
    private static class ExplicitEntityCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "explicit-entity";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.ENTITY;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            return type.annotations().stream()
                    .filter(a -> a.qualifiedName().endsWith(".Entity"))
                    .findFirst()
                    .map(a -> CriterionMatch.high(
                            "Type has @Entity annotation",
                            Evidence.of(EvidenceType.ANNOTATION, "@Entity annotation found")));
        }
    }

    /** Test criterion for explicit @Identifier annotation. */
    private static class ExplicitIdentifierCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "explicit-identifier";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.IDENTIFIER;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            return type.annotations().stream()
                    .filter(a -> a.qualifiedName().endsWith("Identifier"))
                    .findFirst()
                    .map(a -> CriterionMatch.high(
                            "Type has @Identifier annotation",
                            Evidence.of(EvidenceType.ANNOTATION, "@Identifier annotation found")));
        }
    }

    /** Test criterion for explicit @DomainEvent annotation. */
    private static class ExplicitDomainEventCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "explicit-domain-event";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.DOMAIN_EVENT;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            return type.annotations().stream()
                    .filter(a -> a.qualifiedName().endsWith("DomainEvent"))
                    .findFirst()
                    .map(a -> CriterionMatch.high(
                            "Type has @DomainEvent annotation",
                            Evidence.of(EvidenceType.ANNOTATION, "@DomainEvent annotation found")));
        }
    }

    /** Test criterion for repository dominant type. */
    private static class RepositoryDominantCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "repository-dominant";
        }

        @Override
        public int priority() {
            return 80;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.AGGREGATE_ROOT;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            if (context.isRepositoryDominantType(type.qualifiedName())) {
                return Optional.of(CriterionMatch.medium(
                        "Type is primary type in repository",
                        Evidence.of(EvidenceType.RELATIONSHIP, "Used as primary type in repository")));
            }
            return Optional.empty();
        }
    }

    /** Test criterion for record types as value objects. */
    private static class RecordValueObjectCriterion implements ClassificationCriterion {
        @Override
        public String name() {
            return "record-value-object";
        }

        @Override
        public int priority() {
            return 70;
        }

        @Override
        public ElementKind targetKind() {
            return ElementKind.VALUE_OBJECT;
        }

        @Override
        public Optional<CriterionMatch> evaluate(TypeSyntax type, ClassificationContext context) {
            if (type.form() == TypeForm.RECORD) {
                return Optional.of(CriterionMatch.medium(
                        "Record types are typically value objects",
                        Evidence.of(EvidenceType.STRUCTURE, "Type is a Java record")));
            }
            return Optional.empty();
        }
    }

    // ===== Stubs =====

    /** Stub TypeSyntax for testing. */
    private static class StubTypeSyntax implements TypeSyntax {
        private final String qualifiedName;
        private final String simpleName;
        private final String packageName;
        private TypeForm form = TypeForm.CLASS;
        private List<AnnotationSyntax> annotations = List.of();

        StubTypeSyntax(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            int lastDot = qualifiedName.lastIndexOf('.');
            this.simpleName = lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
            this.packageName = lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
        }

        StubTypeSyntax withAnnotation(String annotationName) {
            this.annotations = List.of(new StubAnnotationSyntax(annotationName));
            return this;
        }

        StubTypeSyntax asRecord() {
            this.form = TypeForm.RECORD;
            return this;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public String packageName() {
            return packageName;
        }

        @Override
        public TypeForm form() {
            return form;
        }

        @Override
        public Set<io.hexaglue.syntax.Modifier> modifiers() {
            return Set.of();
        }

        @Override
        public Optional<io.hexaglue.syntax.TypeRef> superType() {
            return Optional.empty();
        }

        @Override
        public List<io.hexaglue.syntax.TypeRef> interfaces() {
            return List.of();
        }

        @Override
        public List<io.hexaglue.syntax.TypeParameterSyntax> typeParameters() {
            return List.of();
        }

        @Override
        public List<io.hexaglue.syntax.FieldSyntax> fields() {
            return List.of();
        }

        @Override
        public List<io.hexaglue.syntax.MethodSyntax> methods() {
            return List.of();
        }

        @Override
        public List<io.hexaglue.syntax.ConstructorSyntax> constructors() {
            return List.of();
        }

        @Override
        public List<AnnotationSyntax> annotations() {
            return annotations;
        }

        @Override
        public io.hexaglue.syntax.SourceLocation sourceLocation() {
            return io.hexaglue.syntax.SourceLocation.unknown();
        }
    }

    /** Stub AnnotationSyntax for testing. */
    private static class StubAnnotationSyntax implements AnnotationSyntax {
        private final String qualifiedName;
        private final String simpleName;

        StubAnnotationSyntax(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            int lastDot = qualifiedName.lastIndexOf('.');
            this.simpleName = lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public java.util.Map<String, io.hexaglue.syntax.AnnotationValue> values() {
            return java.util.Map.of();
        }
    }

    /** Stub SyntaxProvider for testing. */
    private static class StubSyntaxProvider implements io.hexaglue.syntax.SyntaxProvider {
        @Override
        public java.util.stream.Stream<TypeSyntax> types() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public Optional<TypeSyntax> type(String qualifiedName) {
            return Optional.empty();
        }

        @Override
        public io.hexaglue.syntax.SyntaxMetadata metadata() {
            return new io.hexaglue.syntax.SyntaxMetadata("", List.of(), 0, java.time.Instant.now(), "Stub");
        }

        @Override
        public io.hexaglue.syntax.SyntaxCapabilities capabilities() {
            return io.hexaglue.syntax.SyntaxCapabilities.spoon();
        }
    }
}
