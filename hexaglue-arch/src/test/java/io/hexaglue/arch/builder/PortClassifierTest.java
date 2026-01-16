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
import io.hexaglue.arch.EvidenceType;
import io.hexaglue.arch.builder.criteria.ExplicitDrivenPortCriterion;
import io.hexaglue.arch.builder.criteria.ExplicitDrivingPortCriterion;
import io.hexaglue.arch.builder.criteria.RepositoryInterfaceCriterion;
import io.hexaglue.arch.builder.criteria.UseCaseInterfaceCriterion;
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

@DisplayName("PortClassifier")
class PortClassifierTest {

    private ClassificationContext context;

    @BeforeEach
    void setUp() {
        context = ClassificationContext.empty(new StubSyntaxProvider());
    }

    @Nested
    @DisplayName("Interface Only")
    class InterfaceOnlyTest {

        @Test
        @DisplayName("should only classify interfaces")
        void shouldOnlyClassifyInterfaces() {
            // given
            TypeSyntax classType = new StubTypeSyntax("com.example.OrderRepository", TypeForm.CLASS);
            PortClassifier classifier = PortClassifiers.standard();

            // when
            ClassificationTrace trace = classifier.classify(classType, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should reject enum types")
        void shouldRejectEnumTypes() {
            // given
            TypeSyntax enumType = new StubTypeSyntax("com.example.Status", TypeForm.ENUM);
            PortClassifier classifier = PortClassifiers.standard();

            // when
            ClassificationTrace trace = classifier.classify(enumType, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("should reject record types")
        void shouldRejectRecordTypes() {
            // given
            TypeSyntax recordType = new StubTypeSyntax("com.example.Command", TypeForm.RECORD);
            PortClassifier classifier = PortClassifiers.standard();

            // when
            ClassificationTrace trace = classifier.classify(recordType, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
        }
    }

    @Nested
    @DisplayName("Explicit Annotation Classification")
    class ExplicitAnnotationTest {

        @Test
        @DisplayName("should classify as DRIVING_PORT with @DrivingPort")
        void shouldClassifyAsDrivingPort() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderUseCase", TypeForm.INTERFACE).withAnnotation("DrivingPort");
            PortClassifier classifier = new PortClassifier(List.of(new ExplicitDrivingPortCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should classify as DRIVEN_PORT with @DrivenPort")
        void shouldClassifyAsDrivenPort() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE).withAnnotation("DrivenPort");
            PortClassifier classifier = new PortClassifier(List.of(new ExplicitDrivenPortCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVEN_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("Repository Pattern")
    class RepositoryPatternTest {

        @Test
        @DisplayName("should classify interface with @Repository as DRIVEN_PORT")
        void shouldClassifyWithRepositoryAnnotation() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderStorage", TypeForm.INTERFACE).withAnnotation("Repository");
            PortClassifier classifier = new PortClassifier(List.of(new RepositoryInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVEN_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(trace.winningCriterion().evidence().get(0).type()).isEqualTo(EvidenceType.ANNOTATION);
        }

        @Test
        @DisplayName("should classify interface ending with Repository as DRIVEN_PORT")
        void shouldClassifyByNamingConvention() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE);
            PortClassifier classifier = new PortClassifier(List.of(new RepositoryInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVEN_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(trace.winningCriterion().evidence().get(0).type()).isEqualTo(EvidenceType.NAMING);
        }
    }

    @Nested
    @DisplayName("UseCase Pattern")
    class UseCasePatternTest {

        @Test
        @DisplayName("should classify interface with @UseCase as DRIVING_PORT")
        void shouldClassifyWithUseCaseAnnotation() {
            // given
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderHandler", TypeForm.INTERFACE).withAnnotation("UseCase");
            PortClassifier classifier = new PortClassifier(List.of(new UseCaseInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should classify interface ending with UseCase as DRIVING_PORT")
        void shouldClassifyByUseCaseNaming() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.CreateOrderUseCase", TypeForm.INTERFACE);
            PortClassifier classifier = new PortClassifier(List.of(new UseCaseInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        @DisplayName("should classify interface ending with Command as DRIVING_PORT")
        void shouldClassifyCommandInterface() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.CreateOrderCommand", TypeForm.INTERFACE);
            PortClassifier classifier = new PortClassifier(List.of(new UseCaseInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should classify interface ending with Query as DRIVING_PORT")
        void shouldClassifyQueryInterface() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.GetOrderQuery", TypeForm.INTERFACE);
            PortClassifier classifier = new PortClassifier(List.of(new UseCaseInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should classify interface ending with UseCases (plural) as DRIVING_PORT")
        void shouldClassifyPluralUseCasesInterface() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.TaskUseCases", TypeForm.INTERFACE);
            PortClassifier classifier = new PortClassifier(List.of(new UseCaseInterfaceCriterion()));

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Priority Resolution")
    class PriorityResolutionTest {

        @Test
        @DisplayName("should prefer explicit annotation over naming convention")
        void shouldPreferExplicitOverNaming() {
            // given - interface named Repository but annotated as DrivingPort
            TypeSyntax type =
                    new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE).withAnnotation("DrivingPort");
            PortClassifier classifier = PortClassifiers.standard();

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("Unclassified")
    class UnclassifiedTest {

        @Test
        @DisplayName("should return UNCLASSIFIED for generic interface")
        void shouldReturnUnclassifiedForGenericInterface() {
            // given
            TypeSyntax type = new StubTypeSyntax("com.example.SomeService", TypeForm.INTERFACE);
            PortClassifier classifier = PortClassifiers.standard();

            // when
            ClassificationTrace trace = classifier.classify(type, context);

            // then
            assertThat(trace.classifiedAs()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(trace.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }

    // ===== Test Stubs =====

    private static class StubTypeSyntax implements TypeSyntax {
        private final String qualifiedName;
        private final String simpleName;
        private final String packageName;
        private final TypeForm form;
        private List<AnnotationSyntax> annotations = List.of();

        StubTypeSyntax(String qualifiedName, TypeForm form) {
            this.qualifiedName = qualifiedName;
            int lastDot = qualifiedName.lastIndexOf('.');
            this.simpleName = lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
            this.packageName = lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
            this.form = form;
        }

        StubTypeSyntax withAnnotation(String annotationName) {
            this.annotations = List.of(new StubAnnotationSyntax(annotationName));
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
