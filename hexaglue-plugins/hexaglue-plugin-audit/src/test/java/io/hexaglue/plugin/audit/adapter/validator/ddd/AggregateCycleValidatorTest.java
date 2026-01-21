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
import io.hexaglue.plugin.audit.domain.model.RelationshipEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateCycleValidator}.
 *
 * <p>Validates that circular dependencies between aggregates are correctly detected
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class AggregateCycleValidatorTest {

    private AggregateCycleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AggregateCycleValidator();
    }

    @Test
    @DisplayName("Should pass when no cycles")
    void shouldPass_whenNoCycles() {
        // Given: A -> B -> C (linear)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.A")
                .addAggregateRoot("com.example.domain.B")
                .addAggregateRoot("com.example.domain.C")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.A", "com.example.domain.B")
                .addDependency("com.example.domain.B", "com.example.domain.C")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when direct cycle")
    void shouldFail_whenDirectCycle() {
        // Given: A -> B -> A
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.A")
                .addAggregateRoot("com.example.domain.B")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.A", "com.example.domain.B")
                .addDependency("com.example.domain.B", "com.example.domain.A")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:aggregate-cycle");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.BLOCKER);
        assertThat(violations.get(0).message())
                .contains("Circular dependency")
                .contains("A")
                .contains("B");
    }

    @Test
    @DisplayName("Should fail when indirect cycle")
    void shouldFail_whenIndirectCycle() {
        // Given: A -> B -> C -> A
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.A")
                .addAggregateRoot("com.example.domain.B")
                .addAggregateRoot("com.example.domain.C")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.A", "com.example.domain.B")
                .addDependency("com.example.domain.B", "com.example.domain.C")
                .addDependency("com.example.domain.C", "com.example.domain.A")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:aggregate-cycle");
        assertThat(violations.get(0).message())
                .contains("Circular dependency")
                .contains("A")
                .contains("B")
                .contains("C");
    }

    @Test
    @DisplayName("Should pass when linear dependencies")
    void shouldPass_whenLinearDependencies() {
        // Given: Order -> Customer, Order -> Product (no cycle)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addAggregateRoot("com.example.domain.Product")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Order", "com.example.domain.Product")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when codebase has no aggregates")
    void shouldPass_whenNoAggregates() {
        // Given
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should detect self-cycle")
    void shouldDetectSelfCycle() {
        // Given: A -> A (self reference)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.A")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.A", "com.example.domain.A")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("Circular dependency");
    }

    @Test
    @DisplayName("Should detect multiple cycles")
    void shouldDetectMultipleCycles() {
        // Given: A -> B -> A and C -> D -> C
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.A")
                .addAggregateRoot("com.example.domain.B")
                .addAggregateRoot("com.example.domain.C")
                .addAggregateRoot("com.example.domain.D")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.A", "com.example.domain.B")
                .addDependency("com.example.domain.B", "com.example.domain.A")
                .addDependency("com.example.domain.C", "com.example.domain.D")
                .addDependency("com.example.domain.D", "com.example.domain.C")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should provide relationship evidence")
    void shouldProvideRelationshipEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Customer", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0)).isInstanceOf(RelationshipEvidence.class);

        RelationshipEvidence evidence =
                (RelationshipEvidence) violations.get(0).evidence().get(0);
        assertThat(evidence.description()).contains("Aggregates").contains("consistency boundaries");
        assertThat(evidence.relationships()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:aggregate-cycle");
    }
}
