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

package io.hexaglue.arch.builder.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.EvidenceType;
import io.hexaglue.arch.builder.ClassificationContext;
import io.hexaglue.arch.builder.CriterionMatch;
import io.hexaglue.syntax.AnnotationSyntax;
import io.hexaglue.syntax.AnnotationValue;
import io.hexaglue.syntax.ConstructorSyntax;
import io.hexaglue.syntax.FieldSyntax;
import io.hexaglue.syntax.MethodSyntax;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.SourceLocation;
import io.hexaglue.syntax.SyntaxCapabilities;
import io.hexaglue.syntax.SyntaxMetadata;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeParameterSyntax;
import io.hexaglue.syntax.TypeRef;
import io.hexaglue.syntax.TypeSyntax;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Domain Classification Criteria")
class DomainCriteriaTest {

    private ClassificationContext context;

    @BeforeEach
    void setUp() {
        context = ClassificationContext.empty(new StubSyntaxProvider());
    }

    @Nested
    @DisplayName("ExplicitAggregateRootCriterion")
    class ExplicitAggregateRootCriterionTest {

        private final ExplicitAggregateRootCriterion criterion = new ExplicitAggregateRootCriterion();

        @Test
        @DisplayName("should have correct name and priority")
        void shouldHaveCorrectNameAndPriority() {
            assertThat(criterion.name()).isEqualTo("explicit-aggregate-root");
            assertThat(criterion.priority()).isEqualTo(100);
            assertThat(criterion.targetKind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should match @AggregateRoot annotation")
        void shouldMatchAggregateRootAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order")
                    .withAnnotation("io.hexaglue.ddd.AggregateRoot");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.get().evidence()).hasSize(1);
            assertThat(result.get().evidence().get(0).type()).isEqualTo(EvidenceType.ANNOTATION);
        }

        @Test
        @DisplayName("should match jMolecules @AggregateRoot annotation")
        void shouldMatchJMoleculesAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order")
                    .withAnnotation("org.jmolecules.ddd.annotation.AggregateRoot");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should not match without annotation")
        void shouldNotMatchWithoutAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ExplicitValueObjectCriterion")
    class ExplicitValueObjectCriterionTest {

        private final ExplicitValueObjectCriterion criterion = new ExplicitValueObjectCriterion();

        @Test
        @DisplayName("should match @ValueObject annotation")
        void shouldMatchValueObjectAnnotation() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money").withAnnotation("ValueObject");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("ExplicitEntityCriterion")
    class ExplicitEntityCriterionTest {

        private final ExplicitEntityCriterion criterion = new ExplicitEntityCriterion();

        @Test
        @DisplayName("should match DDD @Entity annotation")
        void shouldMatchDddEntityAnnotation() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderLine").withAnnotation("io.hexaglue.ddd.Entity");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should NOT match JPA @Entity annotation")
        void shouldNotMatchJpaEntityAnnotation() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderLine").withAnnotation("jakarta.persistence.Entity");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("RepositoryDominantTypeCriterion")
    class RepositoryDominantTypeCriterionTest {

        private final RepositoryDominantTypeCriterion criterion = new RepositoryDominantTypeCriterion();

        @Test
        @DisplayName("should have correct priority")
        void shouldHaveCorrectPriority() {
            assertThat(criterion.name()).isEqualTo("repository-dominant-type");
            assertThat(criterion.priority()).isEqualTo(80);
            assertThat(criterion.targetKind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should match repository dominant type")
        void shouldMatchRepositoryDominantType() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Order");
            ClassificationContext ctxWithRepo = ClassificationContext.builder(new StubSyntaxProvider())
                    .repositoryDominantTypes(Set.of("com.example.Order"))
                    .build();

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, ctxWithRepo);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.get().evidence().get(0).type()).isEqualTo(EvidenceType.RELATIONSHIP);
        }

        @Test
        @DisplayName("should not match non-repository type")
        void shouldNotMatchNonRepositoryType() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("RecordValueObjectCriterion")
    class RecordValueObjectCriterionTest {

        private final RecordValueObjectCriterion criterion = new RecordValueObjectCriterion();

        @Test
        @DisplayName("should have correct priority")
        void shouldHaveCorrectPriority() {
            assertThat(criterion.name()).isEqualTo("record-value-object");
            assertThat(criterion.priority()).isEqualTo(70);
            assertThat(criterion.targetKind()).isEqualTo(ElementKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should match record types")
        void shouldMatchRecordTypes() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money").asRecord();

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.get().evidence().get(0).type()).isEqualTo(EvidenceType.STRUCTURE);
        }

        @Test
        @DisplayName("should not match class types")
        void shouldNotMatchClassTypes() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.Money");

            // when
            Optional<CriterionMatch> result = criterion.evaluate(type, context);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ===== Test Stubs =====

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
        public Set<Modifier> modifiers() {
            return Set.of();
        }

        @Override
        public Optional<TypeRef> superType() {
            return Optional.empty();
        }

        @Override
        public List<TypeRef> interfaces() {
            return List.of();
        }

        @Override
        public List<TypeParameterSyntax> typeParameters() {
            return List.of();
        }

        @Override
        public List<FieldSyntax> fields() {
            return List.of();
        }

        @Override
        public List<MethodSyntax> methods() {
            return List.of();
        }

        @Override
        public List<ConstructorSyntax> constructors() {
            return List.of();
        }

        @Override
        public List<AnnotationSyntax> annotations() {
            return annotations;
        }

        @Override
        public SourceLocation sourceLocation() {
            return SourceLocation.unknown();
        }
    }

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
        public Map<String, AnnotationValue> values() {
            return Map.of();
        }
    }

    private static class StubSyntaxProvider implements SyntaxProvider {
        @Override
        public Stream<TypeSyntax> types() {
            return Stream.empty();
        }

        @Override
        public Optional<TypeSyntax> type(String qualifiedName) {
            return Optional.empty();
        }

        @Override
        public SyntaxMetadata metadata() {
            return new SyntaxMetadata("", List.of(), 0, Instant.now(), "Stub");
        }

        @Override
        public SyntaxCapabilities capabilities() {
            return SyntaxCapabilities.spoon();
        }
    }
}
