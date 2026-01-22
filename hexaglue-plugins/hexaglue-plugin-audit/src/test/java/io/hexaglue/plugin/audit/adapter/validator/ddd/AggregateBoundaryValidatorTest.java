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
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateBoundaryValidator}.
 *
 * <p>Validates that entities within an aggregate are only accessible through the aggregate root.
 * Aggregate membership is determined via the v5 API using Entity.owningAggregate().
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class AggregateBoundaryValidatorTest {

    private AggregateBoundaryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AggregateBoundaryValidator();
    }

    @Test
    @DisplayName("Should return empty list when model has no domain index")
    void shouldReturnEmptyList_whenModelHasNoDomainIndex() {
        // Given - empty model (no domain index)
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when no entities exist")
    void shouldPass_whenNoEntitiesExist() {
        // Given - model with only aggregate root, no entities
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when entity is only accessed through aggregate root")
    void shouldPass_whenEntityAccessedThroughRoot() {
        // Given: Order aggregate contains OrderLine entity, accessed only by Order
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order", List.of("com.example.domain.OrderLine"), List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when entity is accessed from outside aggregate")
    void shouldFail_whenEntityAccessedExternally() {
        // Given: Order aggregate contains OrderLine, but external service accesses OrderLine
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order", List.of("com.example.domain.OrderLine"), List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.billing.InvoiceService", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:aggregate-boundary");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("OrderLine")
                .contains("Order")
                .contains("accessible outside");
        assertThat(violations.get(0).affectedTypes()).containsExactly("com.example.domain.OrderLine");
    }

    @Test
    @DisplayName("Should pass when entity is accessed by another entity in same aggregate")
    void shouldPass_whenEntityAccessedByPeerEntity() {
        // Given: Order aggregate with OrderLine and OrderItem entities that reference each other
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(
                        "com.example.domain.Order",
                        List.of("com.example.domain.OrderLine", "com.example.domain.OrderItem"),
                        List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .addEntity("com.example.domain.OrderItem", "com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.domain.OrderLine", "com.example.domain.OrderItem")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when entity has no owning aggregate")
    void shouldPass_whenEntityHasNoOwningAggregate() {
        // Given: Entity exists but has no owning aggregate (standalone entity)
        ArchitecturalModel model = new TestModelBuilder()
                .addEntity("com.example.audit.AuditLog") // No owning aggregate
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.reporting.ReportService", "com.example.audit.AuditLog")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - no violation because entity has no owning aggregate defined
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should detect multiple violations across different aggregates")
    void shouldDetectMultipleViolations() {
        // Given: Two aggregates with boundary violations
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.order.Order", List.of("com.example.order.OrderLine"), List.of())
                .addEntity("com.example.order.OrderLine", "com.example.order.Order")
                .addAggregateRoot("com.example.customer.Customer", List.of("com.example.customer.Address"), List.of())
                .addEntity("com.example.customer.Address", "com.example.customer.Customer")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.order.Order", "com.example.order.OrderLine")
                .addDependency("com.example.customer.Customer", "com.example.customer.Address")
                .addDependency("com.example.billing.InvoiceService", "com.example.order.OrderLine")
                .addDependency("com.example.billing.InvoiceService", "com.example.customer.Address")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .flatExtracting(Violation::affectedTypes)
                .containsExactlyInAnyOrder("com.example.order.OrderLine", "com.example.customer.Address");
    }

    @Test
    @DisplayName("Should provide structural evidence in violations")
    void shouldProvideStructuralEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order", List.of("com.example.domain.OrderLine"), List.of())
                .addEntity("com.example.domain.OrderLine", "com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.billing.BillingService", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0)).isInstanceOf(StructuralEvidence.class);

        StructuralEvidence evidence =
                (StructuralEvidence) violations.get(0).evidence().get(0);
        assertThat(evidence.description()).contains("aggregate root");
        assertThat(evidence.involvedTypes()).containsExactly("com.example.domain.OrderLine");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:aggregate-boundary");
    }
}
