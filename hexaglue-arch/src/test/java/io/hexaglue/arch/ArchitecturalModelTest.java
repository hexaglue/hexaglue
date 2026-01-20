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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.adapters.DrivenAdapter;
import io.hexaglue.arch.adapters.DrivingAdapter;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ArchitecturalModel")
class ArchitecturalModelTest {

    private static final String PKG = "com.example";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("ProjectContext")
    class ProjectContextTest {

        @Test
        @DisplayName("should create project context")
        void shouldCreate() {
            // when
            ProjectContext ctx = ProjectContext.of("my-app", "com.example", Path.of("src/main/java"));

            // then
            assertThat(ctx.name()).isEqualTo("my-app");
            assertThat(ctx.basePackage()).isEqualTo("com.example");
            assertThat(ctx.version()).isEmpty();
        }

        @Test
        @DisplayName("should create with version")
        void shouldCreateWithVersion() {
            // when
            ProjectContext ctx = ProjectContext.of("my-app", "com.example", Path.of("src"), "1.0.0");

            // then
            assertThat(ctx.version()).contains("1.0.0");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> ProjectContext.of("  ", "com.example", Path.of("src")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("AnalysisMetadata")
    class AnalysisMetadataTest {

        @Test
        @DisplayName("should create metadata")
        void shouldCreate() {
            // when
            AnalysisMetadata meta = new AnalysisMetadata(Instant.now(), Duration.ofMillis(500), "Spoon", 100, "4.0.0");

            // then
            assertThat(meta.parserName()).isEqualTo("Spoon");
            assertThat(meta.typesAnalyzed()).isEqualTo(100);
            assertThat(meta.durationMillis()).isEqualTo(500);
        }

        @Test
        @DisplayName("should reject negative types count")
        void shouldRejectNegativeCount() {
            assertThatThrownBy(() -> new AnalysisMetadata(Instant.now(), Duration.ofMillis(100), "Test", -1, "4.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should use factory for testing")
        void shouldUseTestingFactory() {
            // when
            AnalysisMetadata meta = AnalysisMetadata.forTesting(50);

            // then
            assertThat(meta.typesAnalyzed()).isEqualTo(50);
            assertThat(meta.parserName()).isEqualTo("Test");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should build empty model")
        void shouldBuildEmpty() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);

            // when
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).build();

            // then
            assertThat(model.size()).isZero();
            assertThat(model.project()).isEqualTo(ctx);
        }

        @Test
        @DisplayName("should build model with elements")
        void shouldBuildWithElements() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            ValueObject vo =
                    ValueObject.of(PKG + ".Money", List.of("amount"), highConfidence(ElementKind.VALUE_OBJECT));

            // when
            ArchitecturalModel model =
                    ArchitecturalModel.builder(ctx).add(root).add(vo).build();

            // then
            assertThat(model.size()).isEqualTo(2);
            assertThat(model.domainEntities().count()).isEqualTo(1);
            assertThat(model.valueObjects().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should build with relationships")
        void shouldBuildWithRelationships() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            Aggregate agg = Aggregate.of(
                    PKG + ".OrderAggregate",
                    ElementRef.of(root.id(), DomainEntity.class),
                    highConfidence(ElementKind.AGGREGATE));
            DrivenPort repo = DrivenPort.of(PKG + ".OrderRepository", highConfidence(ElementKind.DRIVEN_PORT));

            // when
            ArchitecturalModel model = ArchitecturalModel.builder(ctx)
                    .add(root)
                    .add(agg)
                    .add(repo)
                    .addManages(repo.id(), agg.id())
                    .build();

            // then
            assertThat(model.repositoryFor(agg.id())).isPresent();
            assertThat(model.hasRepository(agg.id())).isTrue();
        }
    }

    @Nested
    @DisplayName("ElementAccess")
    class ElementAccessTest {

        @Test
        @DisplayName("should access domain elements")
        void shouldAccessDomainElements() {
            // given
            ArchitecturalModel model = createTestModel();

            // then
            assertThat(model.aggregates().count()).isEqualTo(1);
            assertThat(model.domainEntities().count()).isEqualTo(1);
            assertThat(model.valueObjects().count()).isEqualTo(1);
            assertThat(model.domainEvents().count()).isEqualTo(1);
            assertThat(model.domainServices().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should access port elements")
        void shouldAccessPortElements() {
            // given
            ArchitecturalModel model = createTestModel();

            // then
            assertThat(model.drivingPorts().count()).isEqualTo(1);
            assertThat(model.drivenPorts().count()).isEqualTo(1);
            assertThat(model.applicationServices().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should access adapter elements")
        void shouldAccessAdapterElements() {
            // given
            ArchitecturalModel model = createTestModel();

            // then
            assertThat(model.drivingAdapters().count()).isEqualTo(1);
            assertThat(model.drivenAdapters().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should access unclassified types")
        void shouldAccessUnclassifiedTypes() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            UnclassifiedType unknown = UnclassifiedType.of(PKG + ".Unknown", highConfidence(ElementKind.UNCLASSIFIED));

            ArchitecturalModel model =
                    ArchitecturalModel.builder(ctx).add(unknown).build();

            // then
            assertThat(model.unclassifiedTypes().count()).isEqualTo(1);
            assertThat(model.hasUnclassified()).isTrue();
            assertThat(model.unclassifiedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ReferenceResolution")
    class ReferenceResolutionTest {

        @Test
        @DisplayName("should resolve existing reference")
        void shouldResolveExisting() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            ValueObject vo = ValueObject.of(PKG + ".Money", List.of(), highConfidence(ElementKind.VALUE_OBJECT));
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).add(vo).build();

            ElementRef<ValueObject> ref = ElementRef.of(vo.id(), ValueObject.class);

            // when
            Optional<ValueObject> resolved = model.resolve(ref);

            // then
            assertThat(resolved).contains(vo);
        }

        @Test
        @DisplayName("should resolve using get")
        void shouldResolveUsingGet() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            ValueObject vo = ValueObject.of(PKG + ".Money", List.of(), highConfidence(ElementKind.VALUE_OBJECT));
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).add(vo).build();

            ElementRef<ValueObject> ref = ElementRef.of(vo.id(), ValueObject.class);

            // when
            ValueObject resolved = model.get(ref);

            // then
            assertThat(resolved).isEqualTo(vo);
        }

        @Test
        @DisplayName("should throw for missing reference")
        void shouldThrowForMissing() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).build();

            ElementRef<ValueObject> ref = ElementRef.of(ElementId.of(PKG + ".Missing"), ValueObject.class);

            // then
            assertThatThrownBy(() -> model.get(ref)).isInstanceOf(UnresolvedReferenceException.class);
        }
    }

    @Nested
    @DisplayName("RelationshipQueries")
    class RelationshipQueriesTest {

        @Test
        @DisplayName("should find repository for aggregate")
        void shouldFindRepository() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            Aggregate agg = Aggregate.of(
                    PKG + ".OrderAggregate",
                    ElementRef.of(root.id(), DomainEntity.class),
                    highConfidence(ElementKind.AGGREGATE));
            DrivenPort repo = DrivenPort.of(PKG + ".OrderRepository", highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ctx)
                    .add(root)
                    .add(agg)
                    .add(repo)
                    .addManages(repo.id(), agg.id())
                    .build();

            // when
            Optional<DrivenPort> found = model.repositoryFor(agg.id());

            // then
            assertThat(found).contains(repo);
        }

        @Test
        @DisplayName("should return empty for aggregate without repository")
        void shouldReturnEmptyWithoutRepository() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            Aggregate agg = Aggregate.of(
                    PKG + ".OrderAggregate",
                    ElementRef.of(root.id(), DomainEntity.class),
                    highConfidence(ElementKind.AGGREGATE));

            ArchitecturalModel model =
                    ArchitecturalModel.builder(ctx).add(root).add(agg).build();

            // when
            Optional<DrivenPort> found = model.repositoryFor(agg.id());

            // then
            assertThat(found).isEmpty();
            assertThat(model.hasRepository(agg.id())).isFalse();
        }
    }

    @Nested
    @DisplayName("QueryAPI")
    class QueryAPITest {

        @Test
        @DisplayName("should return query interface")
        void shouldReturnQueryInterface() {
            // given
            ArchitecturalModel model = createTestModel();

            // when
            var query = model.query();

            // then
            assertThat(query).isNotNull();
            assertThat(query.aggregates().count()).isEqualTo(1);
            assertThat(query.drivingPorts().count()).isEqualTo(1);
            assertThat(query.drivenPorts().count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("New v4.1.0 API")
    class NewApiTest {

        @Test
        @DisplayName("should support null new components (backward compatibility)")
        void shouldSupportNullNewComponents() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);

            // when - legacy builder usage
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).build();

            // then
            assertThat(model.typeRegistry()).isEmpty();
            assertThat(model.classificationReport()).isEmpty();
            assertThat(model.domainIndex()).isEmpty();
            assertThat(model.portIndex()).isEmpty();
        }

        @Test
        @DisplayName("should return false for hasClassificationIssues when report is null")
        void shouldReturnFalseForHasClassificationIssuesWhenNull() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).build();

            // when
            boolean hasIssues = model.hasClassificationIssues();

            // then
            assertThat(hasIssues).isFalse();
        }

        @Test
        @DisplayName("should return empty list for topRemediations when report is null")
        void shouldReturnEmptyTopRemediationsWhenNull() {
            // given
            ProjectContext ctx = ProjectContext.forTesting("app", PKG);
            ArchitecturalModel model = ArchitecturalModel.builder(ctx).build();

            // when
            var remediations = model.topRemediations(5);

            // then
            assertThat(remediations).isEmpty();
        }
    }

    // === Test Helpers ===

    private ArchitecturalModel createTestModel() {
        ProjectContext ctx = ProjectContext.forTesting("app", PKG);

        // Domain
        DomainEntity root = DomainEntity.aggregateRoot(PKG + ".Order", highConfidence(ElementKind.AGGREGATE_ROOT));
        Aggregate agg = Aggregate.of(
                PKG + ".OrderAggregate",
                ElementRef.of(root.id(), DomainEntity.class),
                highConfidence(ElementKind.AGGREGATE));
        ValueObject vo = ValueObject.of(PKG + ".Money", List.of(), highConfidence(ElementKind.VALUE_OBJECT));
        DomainEvent event = DomainEvent.of(PKG + ".OrderPlaced", highConfidence(ElementKind.DOMAIN_EVENT));
        DomainService svc = DomainService.of(PKG + ".PricingService", highConfidence(ElementKind.DOMAIN_SERVICE));

        // Ports
        DrivingPort drivingPort = DrivingPort.of(PKG + ".PlaceOrderUseCase", highConfidence(ElementKind.DRIVING_PORT));
        DrivenPort drivenPort = DrivenPort.of(PKG + ".OrderRepository", highConfidence(ElementKind.DRIVEN_PORT));
        ApplicationService appSvc = ApplicationService.of(
                PKG + ".OrderApplicationService", highConfidence(ElementKind.APPLICATION_SERVICE));

        // Adapters
        DrivingAdapter drivingAdapter =
                DrivingAdapter.of(PKG + ".OrderController", highConfidence(ElementKind.DRIVING_ADAPTER));
        DrivenAdapter drivenAdapter =
                DrivenAdapter.of(PKG + ".JpaOrderRepository", highConfidence(ElementKind.DRIVEN_ADAPTER));

        return ArchitecturalModel.builder(ctx)
                .add(root)
                .add(agg)
                .add(vo)
                .add(event)
                .add(svc)
                .add(drivingPort)
                .add(drivenPort)
                .add(appSvc)
                .add(drivingAdapter)
                .add(drivenAdapter)
                .build();
    }
}
