/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateConsistencyValidator}.
 */
class AggregateConsistencyValidatorTest {

    private AggregateConsistencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AggregateConsistencyValidator();
    }

    // === Constraint Identity ===

    @Test
    void shouldHaveCorrectConstraintId() {
        assertThat(validator.constraintId()).isEqualTo(ConstraintId.of("ddd:aggregate-consistency"));
    }

    @Test
    void shouldHaveMajorSeverity() {
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    // === Rule 1: Single Ownership ===

    @Test
    void shouldPass_whenEntityBelongsToSingleAggregate() {
        // Given: Order aggregate with OrderLine entity
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit orderLineEntity = entity("OrderLine", true);
        CodeUnit customerAggregate = aggregate("Customer");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(orderAggregate)
                .addUnit(orderLineEntity)
                .addUnit(customerAggregate)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violation - entity belongs to single aggregate
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFail_whenEntityBelongsToMultipleAggregates() {
        // Given: Address entity referenced by both Customer and Order
        CodeUnit customerAggregate = aggregate("Customer");
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit addressEntity = entity("Address", true);

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(customerAggregate)
                .addUnit(orderAggregate)
                .addUnit(addressEntity)
                .addDependency("com.example.domain.Customer", "com.example.domain.Address")
                .addDependency("com.example.domain.Order", "com.example.domain.Address")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.constraintId()).isEqualTo(ConstraintId.of("ddd:aggregate-consistency"));
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
        assertThat(violation.message()).contains("Address", "multiple aggregates", "Customer", "Order");
        assertThat(violation.affectedTypes()).contains("com.example.domain.Address");
    }

    @Test
    void shouldFail_whenEntityReferencedByThreeAggregates() {
        // Given: Shared entity referenced by 3 aggregates
        CodeUnit agg1 = aggregate("Aggregate1");
        CodeUnit agg2 = aggregate("Aggregate2");
        CodeUnit agg3 = aggregate("Aggregate3");
        CodeUnit sharedEntity = entity("SharedEntity", true);

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(agg1)
                .addUnit(agg2)
                .addUnit(agg3)
                .addUnit(sharedEntity)
                .addDependency("com.example.domain.Aggregate1", "com.example.domain.SharedEntity")
                .addDependency("com.example.domain.Aggregate2", "com.example.domain.SharedEntity")
                .addDependency("com.example.domain.Aggregate3", "com.example.domain.SharedEntity")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Single violation listing all three aggregates
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message())
                .contains("SharedEntity", "Aggregate1", "Aggregate2", "Aggregate3");
    }

    // === Rule 2: Size Limit ===

    @Test
    void shouldPass_whenAggregateSizeWithinLimit() {
        // Given: Order aggregate with 3 entities (within limit of 7)
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit entity1 = entity("OrderLine", true);
        CodeUnit entity2 = entity("OrderDiscount", true);
        CodeUnit entity3 = entity("OrderNote", true);

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(orderAggregate)
                .addUnit(entity1)
                .addUnit(entity2)
                .addUnit(entity3)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.domain.Order", "com.example.domain.OrderDiscount")
                .addDependency("com.example.domain.Order", "com.example.domain.OrderNote")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violation
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPass_whenAggregateHasExactlySevenEntities() {
        // Given: Aggregate with exactly 7 entities (boundary case)
        CodeUnit aggregate = aggregate("LargeAggregate");
        TestCodebaseBuilder builder = new TestCodebaseBuilder().addUnit(aggregate);

        for (int i = 1; i <= 7; i++) {
            CodeUnit entity = entity("Entity" + i, true);
            builder.addUnit(entity).addDependency("com.example.domain.LargeAggregate", entity.qualifiedName());
        }

        Codebase codebase = builder.build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violation at boundary
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldWarn_whenAggregateTooLarge() {
        // Given: Order aggregate with 8 entities (exceeds limit of 7)
        CodeUnit orderAggregate = aggregate("Order");
        TestCodebaseBuilder builder = new TestCodebaseBuilder().addUnit(orderAggregate);

        // Add 8 entities
        for (int i = 1; i <= 8; i++) {
            CodeUnit entity = entity("Entity" + i, true);
            builder.addUnit(entity).addDependency("com.example.domain.Order", entity.qualifiedName());
        }

        Codebase codebase = builder.build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.message()).contains("Order", "8 entities", "maximum: 7", "splitting");
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    void shouldWarn_whenAggregateMassive() {
        // Given: Huge aggregate with 15 entities
        CodeUnit aggregate = aggregate("MassiveAggregate");
        TestCodebaseBuilder builder = new TestCodebaseBuilder().addUnit(aggregate);

        for (int i = 1; i <= 15; i++) {
            CodeUnit entity = entity("Entity" + i, true);
            builder.addUnit(entity)
                    .addDependency("com.example.domain.MassiveAggregate", entity.qualifiedName());
        }

        Codebase codebase = builder.build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Violation with count
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("15 entities");
    }

    // === Rule 3: Boundary Respect ===

    @Test
    void shouldPass_whenOnlyAggregateRootAccessedExternally() {
        // Given: Infrastructure adapter only references aggregate root
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit orderLineEntity = entity("OrderLine", true);
        CodeUnit orderAdapter = infraClass("OrderAdapter");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(orderAggregate)
                .addUnit(orderLineEntity)
                .addUnit(orderAdapter)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.infrastructure.OrderAdapter", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violation - adapter correctly accesses aggregate root
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFail_whenInternalEntityAccessedExternally() {
        // Given: External adapter directly references internal entity
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit orderLineEntity = entity("OrderLine", true);
        CodeUnit orderAdapter = infraClass("OrderAdapter");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(orderAggregate)
                .addUnit(orderLineEntity)
                .addUnit(orderAdapter)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                // Violation: adapter bypasses aggregate root
                .addDependency("com.example.infrastructure.OrderAdapter", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.message())
                .contains("OrderAdapter", "directly references", "OrderLine", "Order", "aggregate root");
        assertThat(violation.affectedTypes()).contains("com.example.infrastructure.OrderAdapter");
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    void shouldFail_whenApplicationLayerBypassesAggregateRoot() {
        // Given: Application service directly accesses internal entity
        CodeUnit customerAggregate = aggregate("Customer");
        CodeUnit addressEntity = entity("Address", true);
        CodeUnit orderService = applicationClass("OrderService");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(customerAggregate)
                .addUnit(addressEntity)
                .addUnit(orderService)
                .addDependency("com.example.domain.Customer", "com.example.domain.Address")
                .addDependency("com.example.application.OrderService", "com.example.domain.Address")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message())
                .contains("OrderService", "Address", "Customer", "Only the aggregate root");
    }

    // === Edge Cases ===

    @Test
    void shouldPass_whenNoAggregates() {
        // Given: Codebase with no aggregates
        Codebase codebase = withUnits(domainClass("SomeClass"), infraClass("SomeAdapter"));

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violations
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPass_whenAggregateHasNoEntities() {
        // Given: Aggregate with no entity dependencies
        CodeUnit aggregate = aggregate("SimpleAggregate");
        Codebase codebase = withUnits(aggregate);

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No violations
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPass_whenDomainLayerReferencesInternalEntity() {
        // Given: Another domain type references entity (domain-to-domain is allowed)
        CodeUnit orderAggregate = aggregate("Order");
        CodeUnit orderLineEntity = entity("OrderLine", true);
        CodeUnit orderService = domainClass("OrderDomainService");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(orderAggregate)
                .addUnit(orderLineEntity)
                .addUnit(orderService)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.domain.OrderDomainService", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: No boundary violation (domain-to-domain is OK, only multi-ownership is checked)
        assertThat(violations)
                .filteredOn(v -> v.message().contains("bypass") || v.message().contains("boundary"))
                .isEmpty();
    }
}
