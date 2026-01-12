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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.aggregate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.StructuralEvidence;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateBoundaryValidator}.
 *
 * <p>Validates that entities within an aggregate are only accessible through the aggregate root.
 * Aggregate membership is determined by the core via ArchitectureQuery.findAggregateMembership().
 */
class AggregateBoundaryValidatorTest {

    private AggregateBoundaryValidator validator;
    private ArchitectureQuery mockQuery;

    @BeforeEach
    void setUp() {
        validator = new AggregateBoundaryValidator();
        mockQuery = mock(ArchitectureQuery.class);
    }

    @Test
    @DisplayName("Should return empty list when query is null")
    void shouldReturnEmptyList_whenQueryIsNull() {
        // Given
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when no aggregates with members exist")
    void shouldPass_whenNoAggregatesWithMembers() {
        // Given
        Codebase codebase = new TestCodebaseBuilder().build();
        when(mockQuery.findAggregateMembership()).thenReturn(Map.of());

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when entity is only accessed through aggregate root")
    void shouldPass_whenEntityAccessedThroughRoot() {
        // Given: Order aggregate contains OrderLine entity, accessed only by Order
        CodeUnit order = aggregate("Order");
        CodeUnit orderLine = entityInPackage("OrderLine", "com.example.domain");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(orderLine)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // Core says OrderLine belongs to Order aggregate
        when(mockQuery.findAggregateMembership())
                .thenReturn(Map.of("com.example.domain.Order", List.of("com.example.domain.OrderLine")));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when entity is accessed from outside aggregate")
    void shouldFail_whenEntityAccessedExternally() {
        // Given: Order aggregate contains OrderLine, but external service accesses OrderLine
        CodeUnit order = aggregate("Order");
        CodeUnit orderLine = entityInPackage("OrderLine", "com.example.domain");
        CodeUnit externalService = serviceInPackage("InvoiceService", "com.example.billing");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(orderLine)
                .addUnit(externalService)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.billing.InvoiceService", "com.example.domain.OrderLine")
                .build();

        when(mockQuery.findAggregateMembership())
                .thenReturn(Map.of("com.example.domain.Order", List.of("com.example.domain.OrderLine")));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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
        CodeUnit order = aggregate("Order");
        CodeUnit orderLine = entityInPackage("OrderLine", "com.example.domain");
        CodeUnit orderItem = entityInPackage("OrderItem", "com.example.domain");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(orderLine)
                .addUnit(orderItem)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.domain.OrderLine", "com.example.domain.OrderItem")
                .build();

        // Both entities belong to the Order aggregate
        when(mockQuery.findAggregateMembership())
                .thenReturn(Map.of(
                        "com.example.domain.Order",
                        List.of("com.example.domain.OrderLine", "com.example.domain.OrderItem")));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when entity is not part of any aggregate per core")
    void shouldPass_whenEntityNotInAnyAggregate() {
        // Given: Entity exists but core doesn't include it in any aggregate
        CodeUnit order = aggregateInPackage("Order", "com.example.domain");
        CodeUnit standaloneEntity = entityInPackage("AuditLog", "com.example.audit");
        CodeUnit externalService = serviceInPackage("ReportService", "com.example.reporting");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(standaloneEntity)
                .addUnit(externalService)
                .addDependency("com.example.reporting.ReportService", "com.example.audit.AuditLog")
                .build();

        // Core says Order has no members, AuditLog is not in any aggregate
        when(mockQuery.findAggregateMembership()).thenReturn(Map.of());

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should detect multiple violations across different aggregates")
    void shouldDetectMultipleViolations() {
        // Given: Two aggregates with boundary violations
        CodeUnit order = aggregateInPackage("Order", "com.example.order");
        CodeUnit orderLine = entityInPackage("OrderLine", "com.example.order");
        CodeUnit customer = aggregateInPackage("Customer", "com.example.customer");
        CodeUnit address = entityInPackage("Address", "com.example.customer");
        CodeUnit externalService = serviceInPackage("InvoiceService", "com.example.billing");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(orderLine)
                .addUnit(customer)
                .addUnit(address)
                .addUnit(externalService)
                .addDependency("com.example.order.Order", "com.example.order.OrderLine")
                .addDependency("com.example.customer.Customer", "com.example.customer.Address")
                .addDependency("com.example.billing.InvoiceService", "com.example.order.OrderLine")
                .addDependency("com.example.billing.InvoiceService", "com.example.customer.Address")
                .build();

        when(mockQuery.findAggregateMembership())
                .thenReturn(Map.of(
                        "com.example.order.Order", List.of("com.example.order.OrderLine"),
                        "com.example.customer.Customer", List.of("com.example.customer.Address")));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .flatExtracting(v -> v.affectedTypes())
                .containsExactlyInAnyOrder("com.example.order.OrderLine", "com.example.customer.Address");
    }

    @Test
    @DisplayName("Should provide structural evidence in violations")
    void shouldProvideStructuralEvidence() {
        // Given
        CodeUnit order = aggregate("Order");
        CodeUnit orderLine = entityInPackage("OrderLine", "com.example.domain");
        CodeUnit externalService = serviceInPackage("BillingService", "com.example.billing");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(orderLine)
                .addUnit(externalService)
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .addDependency("com.example.billing.BillingService", "com.example.domain.OrderLine")
                .build();

        when(mockQuery.findAggregateMembership())
                .thenReturn(Map.of("com.example.domain.Order", List.of("com.example.domain.OrderLine")));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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

    // === Helper Methods ===

    private CodeUnit entityInPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    private CodeUnit aggregateInPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    private CodeUnit serviceInPackage(String simpleName, String packageName) {
        String qualifiedName = packageName + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.SERVICE,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    private CodeMetrics defaultMetrics() {
        return new CodeMetrics(50, 5, 3, 2, 80.0);
    }

    private DocumentationInfo defaultDocumentation() {
        return new DocumentationInfo(true, 100, List.of());
    }
}
