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

import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.IssueEntry;
import io.hexaglue.plugin.audit.domain.model.report.Suggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IssueEnricher}.
 *
 * @since 5.0.0
 */
@DisplayName("IssueEnricher")
class IssueEnricherTest {

    private final IssueEnricher enricher = new IssueEnricher();

    @Nested
    @DisplayName("Layer isolation template")
    class LayerIsolationTemplate {

        @Test
        @DisplayName("should provide specific impact and remediation for layer-isolation violations")
        void shouldProvideSpecificTemplateForLayerIsolation() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:layer-isolation"))
                    .severity(Severity.MAJOR)
                    .message("Direct dependency from adapter to domain bypassing port")
                    .affectedType("com.example.adapter.OrderJpaAdapter")
                    .location(SourceLocation.of("OrderJpaAdapter.java", 42, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact()).contains("hexagonal architecture contract");
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.action()).contains("port");
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.steps()).hasSize(4);
            assertThat(suggestion.effortOpt()).isPresent().hasValue("1 day");
        }
    }

    @Nested
    @DisplayName("Default template fallback")
    class DefaultTemplateFallback {

        @Test
        @DisplayName("should use generic template for unknown constraint IDs")
        void shouldUseGenericTemplateForUnknownConstraintIds() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("custom:unknown-rule"))
                    .severity(Severity.MINOR)
                    .message("Some unknown violation")
                    .affectedType("com.example.SomeClass")
                    .location(SourceLocation.of("SomeClass.java", 10, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Should use the generic fallback
            assertThat(entry.impact()).contains("may affect the architectural integrity");
            assertThat(entry.suggestion().action()).isEqualTo("Review and fix this issue");
            assertThat(entry.suggestion().hasSteps()).isFalse();
            assertThat(entry.suggestion().effortOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Known templates")
    class KnownTemplates {

        @Test
        @DisplayName("should provide specific template for port-direction violations")
        void shouldProvideSpecificTemplateForPortDirection() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:port-direction"))
                    .severity(Severity.MAJOR)
                    .message("Port direction mismatch")
                    .affectedType("com.example.port.OrderPort")
                    .location(SourceLocation.of("OrderPort.java", 5, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Should use the port-direction template, not the generic one
            assertThat(entry.impact()).contains("direction");
            assertThat(entry.suggestion().action()).contains("port usage direction");
        }
    }

    @Nested
    @DisplayName("Port coverage template")
    class PortCoverageTemplate {

        @Test
        @DisplayName("should suggest automatable REST adapter for driving port without adapter")
        void shouldSuggestAutomatableRestAdapterForDrivingPort() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:port-coverage"))
                    .severity(Severity.MAJOR)
                    .message("Driving port OrderService has no adapter")
                    .affectedType("com.example.port.OrderService")
                    .location(SourceLocation.of("OrderService.java", 5, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.action()).contains("REST API");
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent().hasValue("2 days");
            assertThat(suggestion.isAutomatableByHexaglue()).isTrue();
            assertThat(suggestion.hexagluePluginOpt()).isPresent().hasValue("hexaglue-plugin-rest");
        }

        @Test
        @DisplayName("should suggest automatable JPA adapter for driven port without adapter")
        void shouldSuggestAutomatableJpaAdapterForDrivenPort() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:port-coverage"))
                    .severity(Severity.MAJOR)
                    .message("Driven port OrderRepository has no adapter")
                    .affectedType("com.example.port.OrderRepository")
                    .location(SourceLocation.of("OrderRepository.java", 5, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.action()).contains("infrastructure adapter");
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent().hasValue("3 days");
            assertThat(suggestion.isAutomatableByHexaglue()).isTrue();
            assertThat(suggestion.hexagluePluginOpt()).isPresent().hasValue("hexaglue-plugin-jpa");
        }
    }

    @Nested
    @DisplayName("All constraint templates coverage")
    class AllConstraintTemplates {

        @Test
        @DisplayName("should provide specific template for ddd:entity-identity violations")
        void shouldProvideSpecificTemplateForEntityIdentity() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:entity-identity"))
                    .severity(Severity.MAJOR)
                    .message("Entity lacks explicit identity field")
                    .affectedType("com.example.domain.Customer")
                    .location(SourceLocation.of("Customer.java", 10, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact()).contains("identity");
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:aggregate-repository violations")
        void shouldProvideSpecificTemplateForAggregateRepository() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:aggregate-repository"))
                    .severity(Severity.MAJOR)
                    .message("Aggregate root without repository")
                    .affectedType("com.example.domain.Order")
                    .location(SourceLocation.of("Order.java", 15, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(impact -> assertThat(impact).contains("repository"), impact -> assertThat(impact)
                            .contains("retrieval"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:aggregate-boundary violations")
        void shouldProvideSpecificTemplateForAggregateBoundary() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:aggregate-boundary"))
                    .severity(Severity.MAJOR)
                    .message("Aggregate boundary violation detected")
                    .affectedType("com.example.domain.Order")
                    .location(SourceLocation.of("Order.java", 20, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(
                            impact -> assertThat(impact).contains("aggregate root"),
                            impact -> assertThat(impact).contains("invariants"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:aggregate-consistency multi-ownership case")
        void shouldProvideSpecificTemplateForAggregateConsistencyMultiOwnership() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:aggregate-consistency"))
                    .severity(Severity.MAJOR)
                    .message("Entity OrderItem owned by multiple aggregates")
                    .affectedType("com.example.domain.OrderItem")
                    .location(SourceLocation.of("OrderItem.java", 25, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific to ownership, not the generic fallback
            assertThat(entry.impact()).contains("ownership");
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:aggregate-consistency size case")
        void shouldProvideSpecificTemplateForAggregateConsistencySize() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:aggregate-consistency"))
                    .severity(Severity.MAJOR)
                    .message("Aggregate Order contains 12 entities (threshold: 7)")
                    .affectedType("com.example.domain.Order")
                    .location(SourceLocation.of("Order.java", 30, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific to size, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(impact -> assertThat(impact).contains("oversized"), impact -> assertThat(impact)
                            .contains("performance"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:domain-purity violations")
        void shouldProvideSpecificTemplateForDomainPurity() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:domain-purity"))
                    .severity(Severity.CRITICAL)
                    .message("Domain class depends on infrastructure")
                    .affectedType("com.example.domain.Order")
                    .location(SourceLocation.of("Order.java", 35, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(
                            impact -> assertThat(impact).contains("infrastructure"),
                            impact -> assertThat(impact).contains("framework"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for ddd:event-naming violations")
        void shouldProvideSpecificTemplateForEventNaming() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("ddd:event-naming"))
                    .severity(Severity.MINOR)
                    .message("Event name should be in past tense")
                    .affectedType("com.example.domain.OrderCreate")
                    .location(SourceLocation.of("OrderCreate.java", 5, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact()).contains("past tense");
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for hexagonal:port-interface violations")
        void shouldProvideSpecificTemplateForPortInterface() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:port-interface"))
                    .severity(Severity.MAJOR)
                    .message("Port must be an interface")
                    .affectedType("com.example.port.OrderRepository")
                    .location(SourceLocation.of("OrderRepository.java", 10, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact()).contains("interface");
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for hexagonal:dependency-direction violations")
        void shouldProvideSpecificTemplateForDependencyDirection() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:dependency-direction"))
                    .severity(Severity.CRITICAL)
                    .message("Dependency points outward from domain core")
                    .affectedType("com.example.domain.Order")
                    .location(SourceLocation.of("Order.java", 15, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(impact -> assertThat(impact).contains("inward"), impact -> assertThat(impact)
                            .contains("domain core"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for hexagonal:dependency-inversion violations")
        void shouldProvideSpecificTemplateForDependencyInversion() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:dependency-inversion"))
                    .severity(Severity.MAJOR)
                    .message("Domain depends on concrete implementation")
                    .affectedType("com.example.domain.OrderService")
                    .location(SourceLocation.of("OrderService.java", 20, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(impact -> assertThat(impact).contains("concrete"), impact -> assertThat(impact)
                            .contains("abstraction"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }

        @Test
        @DisplayName("should provide specific template for hexagonal:application-purity violations")
        void shouldProvideSpecificTemplateForApplicationPurity() {
            // Given
            Violation violation = Violation.builder(ConstraintId.of("hexagonal:application-purity"))
                    .severity(Severity.MAJOR)
                    .message("Application service depends on framework code")
                    .affectedType("com.example.application.OrderApplicationService")
                    .location(SourceLocation.of("OrderApplicationService.java", 25, 1))
                    .build();

            // When
            IssueEntry entry = enricher.enrich(violation);

            // Then - Impact should be specific, not the generic fallback
            assertThat(entry.impact())
                    .satisfiesAnyOf(impact -> assertThat(impact).contains("framework"), impact -> assertThat(impact)
                            .contains("infrastructure"));
            assertThat(entry.impact()).doesNotContain("may affect the architectural integrity");

            // Then - Suggestion should have concrete steps and effort
            Suggestion suggestion = entry.suggestion();
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.effortOpt()).isPresent();
        }
    }
}
