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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.Identifier;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ArchitecturalModelBuilder")
@SuppressWarnings("deprecation") // Tests use deprecated unclassifiedTypes() to verify classification behavior
class ArchitecturalModelBuilderTest {

    @Nested
    @DisplayName("Builder Configuration")
    class BuilderConfigurationTest {

        @Test
        @DisplayName("should use default classifiers when not specified")
        void shouldUseDefaultClassifiers() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider();

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("TestProject")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model).isNotNull();
            assertThat(model.project().name()).isEqualTo("TestProject");
            assertThat(model.project().basePackage()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("should accept custom domain classifier")
        void shouldAcceptCustomDomainClassifier() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(new StubTypeSyntax("com.example.Order", TypeForm.CLASS));
            DomainClassifier customClassifier = DomainClassifiers.explicitOnly();

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .domainClassifier(customClassifier)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then - custom classifier should reject class without explicit annotation
            assertThat(model.unclassifiedTypes().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should accept custom port classifier")
        void shouldAcceptCustomPortClassifier() {
            // given
            SyntaxProvider provider =
                    new StubSyntaxProvider(new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE));
            PortClassifier customClassifier = PortClassifiers.explicitOnly();

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .portClassifier(customClassifier)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then - explicit-only classifier should reject interface without annotation
            assertThat(model.unclassifiedTypes().count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Classification Routing")
    class ClassificationRoutingTest {

        @Test
        @DisplayName("should route interfaces to port classifier")
        void shouldRouteInterfacesToPortClassifier() {
            // given
            SyntaxProvider provider =
                    new StubSyntaxProvider(new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then - standard port classifier should classify *Repository as DRIVEN_PORT
            var drivenPorts = model.registry().all(DrivenPort.class).toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().qualifiedName()).isEqualTo("com.example.OrderRepository");
        }

        @Test
        @DisplayName("should route classes to domain classifier")
        void shouldRouteClassesToDomainClassifier() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.OrderPlaced", TypeForm.CLASS).withAnnotation("DomainEvent"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(DomainEvent.class).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should route records to domain classifier")
        void shouldRouteRecordsToDomainClassifier() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(new StubTypeSyntax("com.example.Money", TypeForm.RECORD));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then - records should be classified as VALUE_OBJECT by default
            assertThat(model.registry().all(ValueObject.class).count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Element Creation")
    class ElementCreationTest {

        @Test
        @DisplayName("should create DomainEntity for @AggregateRoot")
        void shouldCreateDomainEntityForAggregateRoot() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.Order", TypeForm.CLASS).withAnnotation("AggregateRoot"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            var aggregateRoots =
                    model.registry().all(DomainEntity.class).filter(DomainEntity::isAggregateRoot).toList();
            assertThat(aggregateRoots).hasSize(1);
            assertThat(aggregateRoots.get(0).kind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should create DomainEntity for @Entity (DDD)")
        void shouldCreateDomainEntityForEntity() {
            // given - must use fully qualified DDD Entity annotation
            SyntaxProvider provider = new StubSyntaxProvider(new StubTypeSyntax("com.example.OrderLine", TypeForm.CLASS)
                    .withAnnotation("io.hexaglue.ddd.Entity"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            var entities =
                    model.registry().all(DomainEntity.class).filter(e -> !e.isAggregateRoot()).toList();
            assertThat(entities).hasSize(1);
            assertThat(entities.get(0).kind()).isEqualTo(ElementKind.ENTITY);
        }

        @Test
        @DisplayName("should create ValueObject for @ValueObject")
        void shouldCreateValueObjectForAnnotated() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.Address", TypeForm.CLASS).withAnnotation("ValueObject"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(ValueObject.class).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create Identifier for @Identifier")
        void shouldCreateIdentifier() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.OrderId", TypeForm.RECORD).withAnnotation("Identifier"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(Identifier.class).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create DomainEvent for @DomainEvent")
        void shouldCreateDomainEvent() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.OrderPlaced", TypeForm.RECORD).withAnnotation("DomainEvent"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(DomainEvent.class).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create DrivingPort for @DrivingPort")
        void shouldCreateDrivingPort() {
            // given
            SyntaxProvider provider =
                    new StubSyntaxProvider(new StubTypeSyntax("com.example.PlaceOrderUseCase", TypeForm.INTERFACE)
                            .withAnnotation("DrivingPort"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(DrivingPort.class).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create DrivenPort for @DrivenPort")
        void shouldCreateDrivenPort() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.PaymentGateway", TypeForm.INTERFACE).withAnnotation("DrivenPort"));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(DrivenPort.class).count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTest {

        @Test
        @DisplayName("should populate analysis metadata")
        void shouldPopulateAnalysisMetadata() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.Order", TypeForm.CLASS),
                    new StubTypeSyntax("com.example.Customer", TypeForm.CLASS));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.analysisMetadata().typesAnalyzed()).isEqualTo(2);
            assertThat(model.analysisMetadata().parserName()).isEqualTo("Stub");
            assertThat(model.analysisMetadata().analysisTime()).isNotNull();
            assertThat(model.analysisMetadata().duration()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Multiple Types")
    class MultipleTypesTest {

        @Test
        @DisplayName("should classify multiple types correctly")
        void shouldClassifyMultipleTypesCorrectly() {
            // given
            SyntaxProvider provider = new StubSyntaxProvider(
                    new StubTypeSyntax("com.example.Order", TypeForm.CLASS).withAnnotation("AggregateRoot"),
                    new StubTypeSyntax("com.example.OrderId", TypeForm.RECORD).withAnnotation("Identifier"),
                    new StubTypeSyntax("com.example.OrderRepository", TypeForm.INTERFACE),
                    new StubTypeSyntax("com.example.PlaceOrderUseCase", TypeForm.INTERFACE));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            assertThat(model.registry().all(DomainEntity.class).filter(DomainEntity::isAggregateRoot).count())
                    .isEqualTo(1);
            assertThat(model.registry().all(Identifier.class).count()).isEqualTo(1);
            assertThat(model.registry().all(DrivenPort.class).count()).isEqualTo(1); // *Repository
            assertThat(model.registry().all(DrivingPort.class).count()).isEqualTo(1); // *UseCase
        }
    }

    @Nested
    @DisplayName("Unclassified Types")
    class UnclassifiedTypesTest {

        @Test
        @DisplayName("should track unclassified types")
        void shouldTrackUnclassifiedTypes() {
            // given
            SyntaxProvider provider =
                    new StubSyntaxProvider(new StubTypeSyntax("com.example.SomeHelper", TypeForm.CLASS));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            var unclassified = model.unclassifiedTypes().toList();
            assertThat(unclassified).hasSize(1);
            assertThat(unclassified.get(0).id().qualifiedName()).isEqualTo("com.example.SomeHelper");
        }

        @Test
        @DisplayName("should provide reason for unclassified types")
        void shouldProvideReasonForUnclassifiedTypes() {
            // given
            SyntaxProvider provider =
                    new StubSyntaxProvider(new StubTypeSyntax("com.example.SomeHelper", TypeForm.CLASS));

            // when
            ArchitecturalModel model = ArchitecturalModelBuilder.builder(provider)
                    .projectName("Test")
                    .basePackage("com.example")
                    .build();

            // then
            var unclassified = model.unclassifiedTypes().toList();
            assertThat(unclassified.get(0).reason()).isNotBlank();
        }
    }

    // ===== Test Stubs =====

    private static class StubTypeSyntax implements TypeSyntax {
        private final String qualifiedName;
        private final String simpleName;
        private final String packageName;
        private final TypeForm form;
        private List<AnnotationSyntax> annotations = List.of();
        private List<FieldSyntax> fields = List.of();

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
            return fields;
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
        private final List<TypeSyntax> types;

        StubSyntaxProvider(TypeSyntax... types) {
            this.types = new ArrayList<>(List.of(types));
        }

        @Override
        public Stream<TypeSyntax> types() {
            return types.stream();
        }

        @Override
        public Optional<TypeSyntax> type(String qualifiedName) {
            return types.stream()
                    .filter(t -> t.qualifiedName().equals(qualifiedName))
                    .findFirst();
        }

        @Override
        public SyntaxMetadata metadata() {
            return new SyntaxMetadata("", List.of(), types.size(), Instant.now(), "Stub");
        }

        @Override
        public SyntaxCapabilities capabilities() {
            return SyntaxCapabilities.spoon();
        }
    }
}
