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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateConsistencyValidator}.
 *
 * <p>Validates aggregate consistency rules using the v5 ArchType API:
 * <ul>
 *   <li>Single ownership: entities should belong to only one aggregate</li>
 *   <li>Size limits: aggregates should not exceed 7 entities</li>
 * </ul>
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class AggregateConsistencyValidatorTest {

    private AggregateConsistencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AggregateConsistencyValidator();
    }

    // === Constraint Identity ===

    @Test
    @DisplayName("Should have correct constraint ID")
    void shouldHaveCorrectConstraintId() {
        assertThat(validator.constraintId()).isEqualTo(ConstraintId.of("ddd:aggregate-consistency"));
    }

    @Test
    @DisplayName("Should have MAJOR severity")
    void shouldHaveMajorSeverity() {
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    // === Rule 1: Single Ownership ===

    @Test
    @DisplayName("Should pass when entity belongs to single aggregate")
    void shouldPass_whenEntityBelongsToSingleAggregate() {
        // Given: Order aggregate with OrderLine entity
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order", List.of("com.example.domain.OrderLine"), List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violation - entity belongs to single aggregate
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when entity belongs to multiple aggregates")
    void shouldFail_whenEntityBelongsToMultipleAggregates() {
        // Given: Address entity referenced by both Customer and Order
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Customer", List.of("com.example.domain.Address"), List.of())
                .addAggregateRoot("com.example.domain.Order", List.of("com.example.domain.Address"), List.of())
                .addEntity("com.example.domain.Address")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.constraintId()).isEqualTo(ConstraintId.of("ddd:aggregate-consistency"));
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
        assertThat(violation.message()).contains("Address", "multiple aggregates", "Customer", "Order");
        assertThat(violation.affectedTypes()).contains("com.example.domain.Address");
    }

    @Test
    @DisplayName("Should fail when entity referenced by three aggregates")
    void shouldFail_whenEntityReferencedByThreeAggregates() {
        // Given: Shared entity referenced by 3 aggregates
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(
                        "com.example.domain.Aggregate1", List.of("com.example.domain.SharedEntity"), List.of())
                .addAggregateRoot(
                        "com.example.domain.Aggregate2", List.of("com.example.domain.SharedEntity"), List.of())
                .addAggregateRoot(
                        "com.example.domain.Aggregate3", List.of("com.example.domain.SharedEntity"), List.of())
                .addEntity("com.example.domain.SharedEntity")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Single violation listing all three aggregates
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("SharedEntity", "Aggregate1", "Aggregate2", "Aggregate3");
    }

    // === Rule 2: Size Limit ===

    @Test
    @DisplayName("Should pass when aggregate size within limit")
    void shouldPass_whenAggregateSizeWithinLimit() {
        // Given: Order aggregate with 3 entities (within limit of 7)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(
                        "com.example.domain.Order",
                        List.of(
                                "com.example.domain.OrderLine",
                                "com.example.domain.OrderDiscount",
                                "com.example.domain.OrderNote"),
                        List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .addEntity("com.example.domain.OrderDiscount", "com.example.domain.Order")
                .addEntity("com.example.domain.OrderNote", "com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violation
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when aggregate has exactly seven entities")
    void shouldPass_whenAggregateHasExactlySevenEntities() {
        // Given: Aggregate with exactly 7 entities (boundary case)
        List<String> entityNames = new ArrayList<>();
        TestModelBuilder builder = new TestModelBuilder();

        for (int i = 1; i <= 7; i++) {
            String entityQName = "com.example.domain.Entity" + i;
            entityNames.add(entityQName);
            builder.addEntity(entityQName, "com.example.domain.LargeAggregate");
        }

        builder.addAggregateRoot("com.example.domain.LargeAggregate", entityNames, List.of());
        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violation at boundary
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should warn when aggregate too large")
    void shouldWarn_whenAggregateTooLarge() {
        // Given: Order aggregate with 8 entities (exceeds limit of 7)
        List<String> entityNames = new ArrayList<>();
        TestModelBuilder builder = new TestModelBuilder();

        for (int i = 1; i <= 8; i++) {
            String entityQName = "com.example.domain.Entity" + i;
            entityNames.add(entityQName);
            builder.addEntity(entityQName, "com.example.domain.Order");
        }

        builder.addAggregateRoot("com.example.domain.Order", entityNames, List.of());
        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Violation detected
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.message()).contains("Order", "8 entities", "maximum: 7");
        assertThat(violation.severity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should warn when aggregate massive")
    void shouldWarn_whenAggregateMassive() {
        // Given: Huge aggregate with 15 entities
        List<String> entityNames = new ArrayList<>();
        TestModelBuilder builder = new TestModelBuilder();

        for (int i = 1; i <= 15; i++) {
            String entityQName = "com.example.domain.Entity" + i;
            entityNames.add(entityQName);
            builder.addEntity(entityQName, "com.example.domain.MassiveAggregate");
        }

        builder.addAggregateRoot("com.example.domain.MassiveAggregate", entityNames, List.of());
        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Violation with count
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("15 entities");
    }

    // === Edge Cases ===

    @Test
    @DisplayName("Should pass when no aggregates")
    void shouldPass_whenNoAggregates() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when aggregate has no entities")
    void shouldPass_whenAggregateHasNoEntities() {
        // Given: Aggregate with no entity dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.SimpleAggregate")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violations
        assertThat(violations).isEmpty();
    }
}
