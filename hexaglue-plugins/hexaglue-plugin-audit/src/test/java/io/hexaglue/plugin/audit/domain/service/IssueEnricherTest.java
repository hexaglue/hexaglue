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
            assertThat(suggestion.effortOpt()).isPresent().hasValue("0.5 days");
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
}
