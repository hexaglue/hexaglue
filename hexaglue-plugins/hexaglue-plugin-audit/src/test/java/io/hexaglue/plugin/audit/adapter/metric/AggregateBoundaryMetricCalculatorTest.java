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

package io.hexaglue.plugin.audit.adapter.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateBoundaryMetricCalculator}.
 *
 * <p>Validates that aggregate boundary encapsulation is correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class AggregateBoundaryMetricCalculatorTest {

    private AggregateBoundaryMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AggregateBoundaryMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.boundary");
    }

    @Test
    @DisplayName("Should return 100% when no entities")
    void shouldReturn100Percent_whenNoEntities() {
        // Given: Model with aggregate but no entities
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.order.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: No entities = perfect encapsulation
        assertThat(metric.name()).isEqualTo("aggregate.boundary");
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no entities found");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return 0% when entities but no aggregates")
    void shouldReturn0Percent_whenEntitiesButNoAggregates() {
        // Given: Entity without aggregate
        ArchitecturalModel model =
                new TestModelBuilder().addEntity("com.example.domain.OrderLine").build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: No aggregates = no encapsulation
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no aggregates found");
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should return 100% when all entities encapsulated")
    void shouldReturn100Percent_whenAllEntitiesEncapsulated() {
        // Given: Aggregate with entity in same package, no external dependencies
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order", new String[] {"com.example.domain.order.OrderLine"}, new String[] {});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Entity depends on aggregate (internal to aggregate)
        codebaseBuilder.addDependency("com.example.domain.order.OrderLine", "com.example.domain.order.Order");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: All entities encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return 0% when all entities leak outside")
    void shouldReturn0Percent_whenAllEntitiesLeakOutside() {
        // Given: Entity accessed from outside aggregate
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.OrderLine"},
                new String[] {"com.example.domain.ExternalService"});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // External service depends on entity (violation)
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.OrderLine");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: No entities encapsulated
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should return 50% when half entities encapsulated")
    void shouldReturn50Percent_whenHalfEntitiesEncapsulated() {
        // Given: 2 entities, one properly encapsulated, one leaked
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.OrderLine", "com.example.domain.order.OrderItem"},
                new String[] {"com.example.domain.ExternalService"});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // OrderLine is encapsulated (only aggregate depends on it)
        codebaseBuilder.addDependency("com.example.domain.order.Order", "com.example.domain.order.OrderLine");
        // OrderItem is leaked (external service depends on it)
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.OrderItem");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should allow entity access from same package")
    void shouldAllowEntityAccessFromSamePackage() {
        // Given: Entity accessed by another class in the same package (aggregate boundary)
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.OrderLine"},
                new String[] {"com.example.domain.order.OrderHelper"});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Helper in same package depends on entity (allowed)
        codebaseBuilder.addDependency("com.example.domain.order.OrderHelper", "com.example.domain.order.OrderLine");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Still encapsulated (same package is within boundary)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should allow entity access from sub-package")
    void shouldAllowEntityAccessFromSubPackage() {
        // Given: Entity in parent package, accessed from sub-package
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.OrderLine"},
                new String[] {"com.example.domain.order.validation.OrderValidator"});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Validator in sub-package depends on entity (allowed)
        codebaseBuilder.addDependency(
                "com.example.domain.order.validation.OrderValidator", "com.example.domain.order.OrderLine");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Still encapsulated (sub-package is within boundary)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should allow entities in same aggregate to reference each other")
    void shouldAllowEntitiesInSameAggregateToReferenceEachOther() {
        // Given: Two entities in same aggregate referencing each other
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.OrderLine", "com.example.domain.order.OrderItem"},
                new String[] {});

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Entities reference each other (allowed)
        codebaseBuilder.addDependency("com.example.domain.order.OrderLine", "com.example.domain.order.OrderItem");
        codebaseBuilder.addDependency("com.example.domain.order.OrderItem", "com.example.domain.order.OrderLine");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Both are encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should calculate correctly with multiple aggregates")
    void shouldCalculateCorrectly_withMultipleAggregates() {
        // Given: 2 aggregates, each with 1 entity, one aggregate has a leak
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();

        // Order aggregate (properly encapsulated)
        registryBuilder.add(createAggregateRoot("com.example.domain.order.Order"));
        registryBuilder.add(createEntity("com.example.domain.order.OrderLine", "com.example.domain.order.Order"));

        // Product aggregate (has leak)
        registryBuilder.add(createAggregateRoot("com.example.domain.product.Product"));
        registryBuilder.add(
                createEntity("com.example.domain.product.ProductSpec", "com.example.domain.product.Product"));

        // External service
        registryBuilder.add(createDomainService("com.example.domain.InventoryService"));

        TypeRegistry typeRegistry = registryBuilder.build();
        ArchitecturalModel model = ArchitecturalModel.builder(
                        ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(DomainIndex.from(typeRegistry))
                .portIndex(PortIndex.from(typeRegistry))
                .build();

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // OrderLine is encapsulated
        codebaseBuilder.addDependency("com.example.domain.order.Order", "com.example.domain.order.OrderLine");
        // ProductSpec is leaked
        codebaseBuilder.addDependency("com.example.domain.InventoryService", "com.example.domain.product.ProductSpec");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should not exceed threshold at boundary")
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Exactly 80% encapsulation (4 out of 5 entities encapsulated)
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        registryBuilder.add(createAggregateRoot("com.example.domain.order.Order"));
        for (int i = 1; i <= 5; i++) {
            registryBuilder.add(createEntity("com.example.domain.order.Entity" + i, "com.example.domain.order.Order"));
        }
        registryBuilder.add(createDomainService("com.example.domain.ExternalService"));

        TypeRegistry typeRegistry = registryBuilder.build();
        ArchitecturalModel model = ArchitecturalModel.builder(
                        ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(DomainIndex.from(typeRegistry))
                .portIndex(PortIndex.from(typeRegistry))
                .build();

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Leak only 1 entity (4/5 = 80% encapsulated)
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.Entity5");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Should not exceed (threshold is < 80, not <=)
        assertThat(metric.value()).isEqualTo(80.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should exceed threshold just below boundary")
    void shouldExceedThreshold_justBelowBoundary() {
        // Given: Just below 80% encapsulation (7 out of 10 entities encapsulated)
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        registryBuilder.add(createAggregateRoot("com.example.domain.order.Order"));
        for (int i = 1; i <= 10; i++) {
            registryBuilder.add(createEntity("com.example.domain.order.Entity" + i, "com.example.domain.order.Order"));
        }
        registryBuilder.add(createDomainService("com.example.domain.ExternalService"));

        TypeRegistry typeRegistry = registryBuilder.build();
        ArchitecturalModel model = ArchitecturalModel.builder(
                        ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(DomainIndex.from(typeRegistry))
                .portIndex(PortIndex.from(typeRegistry))
                .build();

        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();
        // Leak 3 entities (7/10 = 70% encapsulated)
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.Entity8");
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.Entity9");
        codebaseBuilder.addDependency("com.example.domain.ExternalService", "com.example.domain.order.Entity10");
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 70% < 80% threshold
        assertThat(metric.value()).isEqualTo(70.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should handle entity in sub-package of aggregate")
    void shouldHandleEntityInSubPackageOfAggregate() {
        // Given: Entity in sub-package of aggregate
        ArchitecturalModel model = createModelWithPackages(
                "com.example.domain.order.Order",
                new String[] {"com.example.domain.order.items.OrderLine"},
                new String[] {});

        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Entity belongs to aggregate and is encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
    }

    // === Helper Methods ===

    private ArchitecturalModel createModelWithPackages(
            String aggregateQName, String[] entityQNames, String[] serviceQNames) {
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();

        registryBuilder.add(createAggregateRoot(aggregateQName));

        for (String entityQName : entityQNames) {
            registryBuilder.add(createEntity(entityQName, aggregateQName));
        }

        for (String serviceQName : serviceQNames) {
            registryBuilder.add(createDomainService(serviceQName));
        }

        TypeRegistry typeRegistry = registryBuilder.build();

        return ArchitecturalModel.builder(ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(DomainIndex.from(typeRegistry))
                .portIndex(PortIndex.from(typeRegistry))
                .build();
    }

    private AggregateRoot createAggregateRoot(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace trace =
                ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test aggregate");

        Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                .modifiers(Set.of(Modifier.PRIVATE))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        return AggregateRoot.builder(id, structure, trace, idField).build();
    }

    private Entity createEntity(String qualifiedName, String owningAggregateQName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test entity");

        Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                .modifiers(Set.of(Modifier.PRIVATE))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        return Entity.of(id, structure, trace, Optional.of(idField), Optional.of(TypeRef.of(owningAggregateQName)));
    }

    private DomainService createDomainService(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace trace =
                ClassificationTrace.highConfidence(ElementKind.DOMAIN_SERVICE, "test", "Test service");

        return DomainService.of(id, structure, trace);
    }
}
