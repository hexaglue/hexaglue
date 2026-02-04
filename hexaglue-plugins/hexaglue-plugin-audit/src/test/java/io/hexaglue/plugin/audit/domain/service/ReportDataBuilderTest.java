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

package io.hexaglue.plugin.audit.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.AdapterComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.PortComponent;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import io.hexaglue.plugin.audit.domain.model.report.ReportData;
import io.hexaglue.plugin.audit.domain.model.report.Verdict;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for adapter detection in {@link ReportDataBuilder}.
 *
 * <p>Validates that the report builder correctly detects adapter implementations
 * for ports via the CompositionIndex's IMPLEMENTS relationships.
 *
 * @since 5.0.0
 */
class ReportDataBuilderTest {

    private final ReportDataBuilder builder = new ReportDataBuilder();

    @Nested
    @DisplayName("Adapter detection from CompositionIndex")
    class AdapterDetectionFromCompositionIndex {

        @Test
        @DisplayName("Should detect adapter from CompositionIndex IMPLEMENTS relationship")
        void shouldDetectAdapterFromCompositionIndex() {
            // Given - Ports with IMPLEMENTS relationships in CompositionIndex
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .addImplements("com.example.adapter.OrderRestController", "com.example.port.OrderService")
                    .addImplements("com.example.adapter.JpaOrderRepository", "com.example.port.OrderRepository")
                    .build();

            // When
            ReportData report = buildReport(model);
            ComponentDetails components = report.architecture().components();

            // Then - Driving port should have adapter detected
            assertThat(components.drivingPorts()).hasSize(1);
            PortComponent drivingPort = components.drivingPorts().get(0);
            assertThat(drivingPort.hasAdapter()).isTrue();
            assertThat(drivingPort.adapter()).isEqualTo("OrderRestController");

            // Then - Driven port should have adapter detected
            assertThat(components.drivenPorts()).hasSize(1);
            PortComponent drivenPort = components.drivenPorts().get(0);
            assertThat(drivenPort.hasAdapter()).isTrue();
            assertThat(drivenPort.adapter()).isEqualTo("JpaOrderRepository");
        }

        @Test
        @DisplayName("Should report no adapter when no IMPLEMENTS relationship exists")
        void shouldReportNoAdapter_whenNoImplementsRelationship() {
            // Given - Ports without any IMPLEMENTS relationships
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .addDrivenPort("com.example.port.PaymentGateway", DrivenPortType.GATEWAY)
                    .build();

            // When
            ReportData report = buildReport(model);
            ComponentDetails components = report.architecture().components();

            // Then - Driving port should have no adapter
            assertThat(components.drivingPorts()).hasSize(1);
            PortComponent drivingPort = components.drivingPorts().get(0);
            assertThat(drivingPort.hasAdapter()).isFalse();
            assertThat(drivingPort.adapter()).isNull();

            // Then - Driven port should have no adapter
            assertThat(components.drivenPorts()).hasSize(1);
            PortComponent drivenPort = components.drivenPorts().get(0);
            assertThat(drivenPort.hasAdapter()).isFalse();
            assertThat(drivenPort.adapter()).isNull();
        }

        @Test
        @DisplayName("Should populate adapter components from IMPLEMENTS relationships")
        void shouldPopulateAdapterComponents() {
            // Given - Two ports with adapters
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .addImplements("com.example.adapter.rest.OrderRestController", "com.example.port.OrderService")
                    .addImplements(
                            "com.example.adapter.persistence.JpaOrderRepository", "com.example.port.OrderRepository")
                    .build();

            // When
            ReportData report = buildReport(model);
            List<AdapterComponent> adapters = report.architecture().components().adapters();

            // Then - Adapters list should be populated
            assertThat(adapters).hasSize(2);
            assertThat(adapters)
                    .extracting(AdapterComponent::name)
                    .containsExactlyInAnyOrder("OrderRestController", "JpaOrderRepository");
            assertThat(adapters)
                    .extracting(AdapterComponent::implementsPort)
                    .containsExactlyInAnyOrder("OrderService", "OrderRepository");
            assertThat(adapters)
                    .extracting(AdapterComponent::type)
                    .containsExactlyInAnyOrder(
                            AdapterComponent.AdapterType.DRIVING, AdapterComponent.AdapterType.DRIVEN);
        }
    }

    @Nested
    @DisplayName("Relationship filtering")
    class RelationshipFiltering {

        @Test
        @DisplayName("Should exclude self-referencing relationships")
        void shouldExcludeSelfReferencingRelationships() {
            // Given - A model where a type references itself (e.g., Order emits OrderCreatedEvent
            // but graph has Order -> Order: emits due to event sourcing)
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .addDomainEvent("com.example.domain.OrderCreatedEvent")
                    .addImplements("com.example.domain.Order", "com.example.domain.Order")
                    .build();

            // When
            ReportData report = buildReport(model);
            List<Relationship> relationships = report.architecture().relationships();

            // Then - No self-referencing relationship should be present
            assertThat(relationships).noneMatch(r -> r.from().equals(r.to()));
        }

        @Test
        @DisplayName("Should exclude MapperImpl from relationships")
        void shouldExcludeMapperImplFromRelationships() {
            // Given - A model with MapStruct-generated mapper implementations
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderMapper")
                    .addImplements("com.example.adapter.OrderMapperImpl", "com.example.port.OrderMapper")
                    .build();

            // When
            ReportData report = buildReport(model);
            List<Relationship> relationships = report.architecture().relationships();

            // Then - MapperImpl should not appear in relationships
            assertThat(relationships).noneMatch(r -> r.from().endsWith("MapperImpl"));
        }
    }

    @Nested
    @DisplayName("Verdict summary alignment with score")
    class VerdictSummaryAlignmentWithScore {

        @Test
        @DisplayName("should report significant issues when score is below 60 (Grade F)")
        void shouldReportSignificantIssuesWhenScoreBelowSixty() {
            // Given - A builder with a calculator that returns a low score
            HealthScoreCalculator lowScoreCalculator = new HealthScoreCalculator() {
                @Override
                public HealthScore calculate(
                        List<Violation> violations,
                        ArchitectureQuery architectureQuery,
                        Set<String> classifiedPackages) {
                    return new HealthScore(54, 40, 50, 60, 70, 80, "F");
                }
            };
            ReportDataBuilder customBuilder = new ReportDataBuilder(lowScoreCalculator, new IssueEnricher());

            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .build();
            Violation majorViolation = Violation.builder(ConstraintId.of("hexagonal:layer-isolation"))
                    .severity(Severity.MAJOR)
                    .message("Layer isolation violated")
                    .affectedType("com.example.Order")
                    .location(SourceLocation.of("Order.java", 1, 1))
                    .build();
            AuditResult auditResult = new AuditResult(List.of(majorViolation), Map.of(), BuildOutcome.SUCCESS);

            // When
            ReportData report = customBuilder.build(
                    null, auditResult, null, model, "test-project", "1.0.0", "5.0.0", "5.0.0", Duration.ofMillis(100));
            Verdict verdict = report.verdict();

            // Then - Summary should reflect the low score
            assertThat(verdict.summary()).contains("significant architectural issues");
        }

        @Test
        @DisplayName("should report notable issues when score is between 60 and 69 (Grade D)")
        void shouldReportNotableIssuesWhenScoreBetweenSixtyAndSixtyNine() {
            // Given
            HealthScoreCalculator midScoreCalculator = new HealthScoreCalculator() {
                @Override
                public HealthScore calculate(
                        List<Violation> violations,
                        ArchitectureQuery architectureQuery,
                        Set<String> classifiedPackages) {
                    return new HealthScore(65, 60, 60, 70, 70, 80, "D");
                }
            };
            ReportDataBuilder customBuilder = new ReportDataBuilder(midScoreCalculator, new IssueEnricher());

            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .build();
            Violation majorViolation = Violation.builder(ConstraintId.of("hexagonal:layer-isolation"))
                    .severity(Severity.MAJOR)
                    .message("Layer isolation violated")
                    .affectedType("com.example.Order")
                    .location(SourceLocation.of("Order.java", 1, 1))
                    .build();
            AuditResult auditResult = new AuditResult(List.of(majorViolation), Map.of(), BuildOutcome.SUCCESS);

            // When
            ReportData report = customBuilder.build(
                    null, auditResult, null, model, "test-project", "1.0.0", "5.0.0", "5.0.0", Duration.ofMillis(100));
            Verdict verdict = report.verdict();

            // Then - Summary should reflect the moderate score
            assertThat(verdict.summary()).contains("notable architectural issues");
        }

        @Test
        @DisplayName("should report generally healthy when score is 70 or above with violations")
        void shouldReportGenerallyHealthyWhenScoreAboveSeventy() {
            // Given
            HealthScoreCalculator goodScoreCalculator = new HealthScoreCalculator() {
                @Override
                public HealthScore calculate(
                        List<Violation> violations,
                        ArchitectureQuery architectureQuery,
                        Set<String> classifiedPackages) {
                    return new HealthScore(75, 70, 70, 80, 80, 90, "C");
                }
            };
            ReportDataBuilder customBuilder = new ReportDataBuilder(goodScoreCalculator, new IssueEnricher());

            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .build();
            Violation minorViolation = Violation.builder(ConstraintId.of("hexagonal:layer-isolation"))
                    .severity(Severity.MINOR)
                    .message("Minor layer issue")
                    .affectedType("com.example.Order")
                    .location(SourceLocation.of("Order.java", 1, 1))
                    .build();
            AuditResult auditResult = new AuditResult(List.of(minorViolation), Map.of(), BuildOutcome.SUCCESS);

            // When
            ReportData report = customBuilder.build(
                    null, auditResult, null, model, "test-project", "1.0.0", "5.0.0", "5.0.0", Duration.ofMillis(100));
            Verdict verdict = report.verdict();

            // Then - Summary should say generally healthy
            assertThat(verdict.summary()).contains("generally healthy");
        }
    }

    /**
     * Builds a report with minimal required parameters for testing component details.
     */
    private ReportData buildReport(ArchitecturalModel model) {
        AuditResult auditResult = new AuditResult(List.of(), Map.of(), BuildOutcome.SUCCESS);
        return builder.build(
                null, auditResult, null, model, "test-project", "1.0.0", "5.0.0", "5.0.0", Duration.ofMillis(100));
    }
}
