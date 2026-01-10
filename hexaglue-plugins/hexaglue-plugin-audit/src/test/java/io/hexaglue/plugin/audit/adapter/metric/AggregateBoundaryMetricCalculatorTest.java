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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateBoundaryMetricCalculator}.
 */
class AggregateBoundaryMetricCalculatorTest {

    private AggregateBoundaryMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AggregateBoundaryMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.boundary");
    }

    @Test
    void shouldReturn100Percent_whenNoEntities() {
        // Given: Codebase with no entities
        CodeUnit aggregate = aggregate("Order");
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: No entities = perfect encapsulation
        assertThat(metric.name()).isEqualTo("aggregate.boundary");
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no entities found");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturn0Percent_whenEntitiesButNoAggregates() {
        // Given: Entities exist but no aggregates
        CodeUnit entity = entity("OrderLine", true);
        Codebase codebase = withUnits(entity);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: No aggregates = no encapsulation
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no aggregates found");
        assertThat(metric.exceedsThreshold()).isTrue(); // Below 80% threshold
    }

    @Test
    void shouldReturn100Percent_whenAllEntitiesEncapsulated() {
        // Given: Aggregate with entity, no external dependencies
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity = createEntityWithPackage("OrderLine", "com.example.domain.order");

        builder.addUnit(aggregate);
        builder.addUnit(entity);

        // Entity depends on aggregate (internal to aggregate)
        builder.addDependency(entity.qualifiedName(), aggregate.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: All entities encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturn0Percent_whenAllEntitiesLeakOutside() {
        // Given: Entity accessed from outside aggregate
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit externalService = domainClass("ExternalService");

        builder.addUnit(aggregate);
        builder.addUnit(entity);
        builder.addUnit(externalService);

        // External service depends on entity (violation)
        builder.addDependency(externalService.qualifiedName(), entity.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: No entities encapsulated
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldReturn50Percent_whenHalfEntitiesEncapsulated() {
        // Given: 2 entities, one properly encapsulated, one leaked
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity1 = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit entity2 = createEntityWithPackage("OrderItem", "com.example.domain.order");
        CodeUnit externalService = domainClass("ExternalService");

        builder.addUnit(aggregate);
        builder.addUnit(entity1);
        builder.addUnit(entity2);
        builder.addUnit(externalService);

        // entity1 is encapsulated (only aggregate depends on it)
        builder.addDependency(aggregate.qualifiedName(), entity1.qualifiedName());

        // entity2 is leaked (external service depends on it)
        builder.addDependency(externalService.qualifiedName(), entity2.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // Below 80%
    }

    @Test
    void shouldAllowEntityAccessFromSamePackage() {
        // Given: Entity accessed by another class in the same package (aggregate boundary)
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit helperClass = createDomainClassWithPackage("OrderHelper", "com.example.domain.order");

        builder.addUnit(aggregate);
        builder.addUnit(entity);
        builder.addUnit(helperClass);

        // Helper in same package depends on entity (allowed)
        builder.addDependency(helperClass.qualifiedName(), entity.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Still encapsulated (same package is within boundary)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldAllowEntityAccessFromSubPackage() {
        // Given: Entity in parent package, accessed from sub-package (still within boundary)
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit helperClass = createDomainClassWithPackage("OrderValidator", "com.example.domain.order.validation");

        builder.addUnit(aggregate);
        builder.addUnit(entity);
        builder.addUnit(helperClass);

        // Helper in sub-package depends on entity (allowed)
        builder.addDependency(helperClass.qualifiedName(), entity.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Still encapsulated (sub-package is within boundary)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldAllowEntitiesInSameAggregateToReferenceEachOther() {
        // Given: Two entities in same aggregate referencing each other
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity1 = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit entity2 = createEntityWithPackage("OrderItem", "com.example.domain.order");

        builder.addUnit(aggregate);
        builder.addUnit(entity1);
        builder.addUnit(entity2);

        // Entities reference each other (allowed)
        builder.addDependency(entity1.qualifiedName(), entity2.qualifiedName());
        builder.addDependency(entity2.qualifiedName(), entity1.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Both are encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateCorrectly_withMultipleAggregates() {
        // Given: 2 aggregates, each with 1 entity, one aggregate has a leak
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        // Order aggregate (properly encapsulated)
        CodeUnit orderAggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit orderLine = createEntityWithPackage("OrderLine", "com.example.domain.order");

        // Product aggregate (has leak)
        CodeUnit productAggregate = createAggregateWithPackage("Product", "com.example.domain.product");
        CodeUnit productSpec = createEntityWithPackage("ProductSpec", "com.example.domain.product");

        CodeUnit externalService = domainClass("InventoryService");

        builder.addUnit(orderAggregate);
        builder.addUnit(orderLine);
        builder.addUnit(productAggregate);
        builder.addUnit(productSpec);
        builder.addUnit(externalService);

        // OrderLine is encapsulated
        builder.addDependency(orderAggregate.qualifiedName(), orderLine.qualifiedName());

        // ProductSpec is leaked
        builder.addDependency(externalService.qualifiedName(), productSpec.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldNotCountEntitiesWithoutAggregates() {
        // Given: Entity that doesn't belong to any aggregate (different package)
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity1 = createEntityWithPackage("OrderLine", "com.example.domain.order");
        CodeUnit orphanEntity = createEntityWithPackage("StandaloneEntity", "com.example.domain.standalone");

        builder.addUnit(aggregate);
        builder.addUnit(entity1);
        builder.addUnit(orphanEntity);

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only 1 entity belongs to aggregate, and it's encapsulated = 100%
        // (orphan entity is excluded from calculation)
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Exactly 80% encapsulation
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");

        // Create 5 entities
        CodeUnit entity1 = createEntityWithPackage("Entity1", "com.example.domain.order");
        CodeUnit entity2 = createEntityWithPackage("Entity2", "com.example.domain.order");
        CodeUnit entity3 = createEntityWithPackage("Entity3", "com.example.domain.order");
        CodeUnit entity4 = createEntityWithPackage("Entity4", "com.example.domain.order");
        CodeUnit entity5 = createEntityWithPackage("Entity5", "com.example.domain.order");

        CodeUnit externalService = domainClass("ExternalService");

        builder.addUnit(aggregate);
        builder.addUnit(entity1);
        builder.addUnit(entity2);
        builder.addUnit(entity3);
        builder.addUnit(entity4);
        builder.addUnit(entity5);
        builder.addUnit(externalService);

        // 4 encapsulated, 1 leaked = 80%
        builder.addDependency(externalService.qualifiedName(), entity5.qualifiedName());

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Should not exceed (threshold is < 80, not <=)
        assertThat(metric.value()).isEqualTo(80.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldExceedThreshold_justBelowBoundary() {
        // Given: Just below 80% encapsulation
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");

        // Create 10 entities
        for (int i = 1; i <= 10; i++) {
            builder.addUnit(createEntityWithPackage("Entity" + i, "com.example.domain.order"));
        }

        CodeUnit externalService = domainClass("ExternalService");
        builder.addUnit(aggregate);
        builder.addUnit(externalService);

        // Leak 3 entities = 7/10 = 70%
        builder.addDependency(externalService.qualifiedName(), "com.example.domain.order.Entity8");
        builder.addDependency(externalService.qualifiedName(), "com.example.domain.order.Entity9");
        builder.addDependency(externalService.qualifiedName(), "com.example.domain.order.Entity10");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 70% < 80% threshold
        assertThat(metric.value()).isEqualTo(70.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldHandleEntityInSubPackageOfAggregate() {
        // Given: Entity in sub-package of aggregate
        TestCodebaseBuilder builder = new TestCodebaseBuilder();

        CodeUnit aggregate = createAggregateWithPackage("Order", "com.example.domain.order");
        CodeUnit entity = createEntityWithPackage("OrderLine", "com.example.domain.order.items");

        builder.addUnit(aggregate);
        builder.addUnit(entity);

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Entity belongs to aggregate and is encapsulated
        assertThat(metric.value()).isEqualTo(100.0);
    }

    // === Helper Methods ===

    /**
     * Creates an aggregate with a specific package.
     */
    private CodeUnit createAggregateWithPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;
        List<io.hexaglue.spi.audit.FieldDeclaration> fields = new ArrayList<>();
        fields.add(new io.hexaglue.spi.audit.FieldDeclaration(
                "id", "java.lang.Long", Set.of("private"), Set.of("javax.persistence.Id")));

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                List.of(),
                fields,
                new CodeMetrics(50, 5, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates an entity with a specific package.
     */
    private CodeUnit createEntityWithPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;
        List<io.hexaglue.spi.audit.FieldDeclaration> fields = new ArrayList<>();
        fields.add(new io.hexaglue.spi.audit.FieldDeclaration(
                "id", "java.lang.Long", Set.of("private"), Set.of("javax.persistence.Id")));

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                List.of(),
                fields,
                new CodeMetrics(50, 5, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates a domain class with a specific package.
     */
    private CodeUnit createDomainClassWithPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.SERVICE,
                List.of(),
                List.of(),
                new CodeMetrics(50, 5, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }
}
