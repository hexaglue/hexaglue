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

package io.hexaglue.plugin.audit.adapter.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.adapter.report.model.ArchitectureAnalysis;
import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.AuditSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ConstraintsSummary;
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ReportMetadata;
import io.hexaglue.plugin.audit.adapter.report.model.TechnicalDebtSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import io.hexaglue.plugin.audit.domain.model.RecommendationPriority;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonReportGenerator}.
 */
class JsonReportGeneratorTest {

    @Test
    void shouldGenerateJsonForPassedAudit() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        JsonReportGenerator generator = new JsonReportGenerator();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"projectName\": \"test-project\"");
        assertThat(json).contains("\"passed\": true");
        assertThat(json).contains("\"totalViolations\": 0");
        assertThat(json).contains("\"violations\": []");
    }

    @Test
    void shouldGenerateJsonWithViolations() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 2, 1, 0, 1, 0, 0);
        ViolationEntry v1 = new ViolationEntry(
                "ddd:entity-identity", "BLOCKER", "Entity must have ID", "Order", "Order.java:1:1", "");
        ViolationEntry v2 = new ViolationEntry(
                "ddd:value-object-immutable", "MAJOR", "Value object must be immutable", "Money", "Money.java:5:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v1, v2), List.of(), new ConstraintsSummary(0, List.of()));

        JsonReportGenerator generator = new JsonReportGenerator();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"passed\": false");
        assertThat(json).contains("\"totalViolations\": 2");
        assertThat(json).contains("\"blockers\": 1");
        assertThat(json).contains("\"majors\": 1");
        assertThat(json).contains("\"ddd:entity-identity\"");
        assertThat(json).contains("\"Entity must have ID\"");
        assertThat(json).contains("\"Order\"");
    }

    @Test
    void shouldGenerateJsonWithMetrics() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        MetricEntry metric = new MetricEntry("aggregate.avgSize", 15.5, "methods", 20.0, "max", "OK");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(metric), new ConstraintsSummary(0, List.of()));

        JsonReportGenerator generator = new JsonReportGenerator();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"name\": \"aggregate.avgSize\"");
        assertThat(json).contains("\"value\": 15.5");
        assertThat(json).contains("\"unit\": \"methods\"");
        assertThat(json).contains("\"threshold\": 20.0");
        assertThat(json).contains("\"thresholdType\": \"max\"");
        assertThat(json).contains("\"status\": \"OK\"");
    }

    @Test
    void shouldEscapeSpecialCharactersInJson() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test\"project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 0, 0, 1, 0, 0);
        ViolationEntry v = new ViolationEntry(
                "test:constraint",
                "MAJOR",
                "Message with \"quotes\" and \n newlines",
                "Type",
                "File.java:1:1",
                "Evidence with \\ backslash");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        JsonReportGenerator generator = new JsonReportGenerator();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\\\"quotes\\\"");
        assertThat(json).contains("\\n");
        assertThat(json).contains("\\\\");
    }

    @Test
    void shouldReturnCorrectFormat() {
        // Given
        JsonReportGenerator generator = new JsonReportGenerator();

        // Then
        assertThat(generator.format()).isEqualTo(ReportFormat.JSON);
    }

    /**
     * Tests for C5 bug fix: JSON must use decimal points, not commas.
     *
     * <p>Bug: In some locales (e.g., French), String.format() produces
     * "0,1234" instead of "0.1234", making JSON invalid.
     *
     * @see <a href="https://github.com/hexaglue/hexaglue/issues/C5">C5</a>
     */
    @Nested
    class DecimalFormattingTest {

        // Pattern to detect decimal numbers with commas (invalid JSON)
        private static final Pattern DECIMAL_WITH_COMMA = Pattern.compile("\"\\w+\":\\s*\\d+,\\d+");

        @Test
        void shouldUseDecimalPointsInCouplingMetrics() {
            // Given - a report with coupling metrics containing decimal values
            var couplingEntry =
                    new ArchitectureAnalysis.PackageCouplingEntry("com.example.domain", 5, 3, 0.375, 0.25, 0.125);
            var analysis = new ArchitectureAnalysis(
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(couplingEntry));

            var report = createReportWithArchitectureAnalysis(analysis);
            var generator = new JsonReportGenerator();

            // When
            String json = generator.generate(report);

            // Then - JSON must contain decimal points, not commas
            assertThat(json)
                    .as("JSON should use decimal points for instability")
                    .contains("\"instability\": 0.3750");
            assertThat(json)
                    .as("JSON should use decimal points for abstractness")
                    .contains("\"abstractness\": 0.2500");
            assertThat(json).as("JSON should use decimal points for distance").contains("\"distance\": 0.1250");
            assertThat(DECIMAL_WITH_COMMA.matcher(json).find())
                    .as("JSON must not contain decimal commas (invalid JSON)")
                    .isFalse();
        }

        @Test
        void shouldUseDecimalPointsInTechnicalDebt() {
            // Given - a report with technical debt containing decimal values
            var debtCategory = new TechnicalDebtSummary.DebtCategory("Dependency Cycles", 2.5, 1250.75, "Fix cycles");
            var debt = new TechnicalDebtSummary(5.25, 2625.50, 131.25, List.of(debtCategory));

            var report = createReportWithTechnicalDebt(debt);
            var generator = new JsonReportGenerator();

            // When
            String json = generator.generate(report);

            // Then - JSON must contain decimal points, not commas
            assertThat(json).as("JSON should use decimal points for totalDays").contains("\"totalDays\": 5.25");
            assertThat(json).as("JSON should use decimal points for totalCost").contains("\"totalCost\": 2625.50");
            assertThat(json)
                    .as("JSON should use decimal points for monthlyInterest")
                    .contains("\"monthlyInterest\": 131.25");
            assertThat(json)
                    .as("JSON should use decimal points for category days")
                    .contains("\"days\": 2.50");
            assertThat(json)
                    .as("JSON should use decimal points for category cost")
                    .contains("\"cost\": 1250.75");
            assertThat(DECIMAL_WITH_COMMA.matcher(json).find())
                    .as("JSON must not contain decimal commas (invalid JSON)")
                    .isFalse();
        }

        @Test
        void shouldUseDecimalPointsInRecommendations() {
            // Given - a report with recommendations containing decimal values
            var recommendation = Recommendation.builder()
                    .id("rec-1")
                    .priority(RecommendationPriority.IMMEDIATE)
                    .title("Fix coupling")
                    .description("Reduce coupling between packages")
                    .affectedTypes(List.of("com.example.A", "com.example.B"))
                    .estimatedEffort(3.75)
                    .expectedImpact("Better maintainability")
                    .relatedViolations(List.of(ConstraintId.of("coupling:high")))
                    .build();

            var report = createReportWithRecommendations(List.of(recommendation));
            var generator = new JsonReportGenerator();

            // When
            String json = generator.generate(report);

            // Then - JSON must contain decimal points, not commas
            assertThat(json)
                    .as("JSON should use decimal points for estimatedEffort")
                    .contains("\"estimatedEffort\": 3.75");
            assertThat(DECIMAL_WITH_COMMA.matcher(json).find())
                    .as("JSON must not contain decimal commas (invalid JSON)")
                    .isFalse();
        }

        @Test
        void shouldProduceValidJsonRegardlessOfSystemLocale() {
            // Given - save current locale and set to French (uses comma as decimal separator)
            Locale originalLocale = Locale.getDefault();
            try {
                Locale.setDefault(Locale.FRANCE);

                var couplingEntry =
                        new ArchitectureAnalysis.PackageCouplingEntry("com.example", 10, 5, 0.3333, 0.6667, 0.0);
                var analysis = new ArchitectureAnalysis(
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(couplingEntry));
                var debt = new TechnicalDebtSummary(1.5, 750.0, 37.5, List.of());
                var report = createReportWithAnalysisAndDebt(analysis, debt);
                var generator = new JsonReportGenerator();

                // When
                String json = generator.generate(report);

                // Then - JSON must still use decimal points
                assertThat(DECIMAL_WITH_COMMA.matcher(json).find())
                        .as("JSON must use decimal points even with French locale")
                        .isFalse();
                assertThat(json)
                        .contains("0.3333")
                        .contains("0.6667")
                        .contains("1.50")
                        .contains("750.00");
            } finally {
                // Restore original locale
                Locale.setDefault(originalLocale);
            }
        }

        private AuditReport createReportWithArchitectureAnalysis(ArchitectureAnalysis analysis) {
            return new AuditReport(
                    createDefaultMetadata(),
                    createPassingSummary(),
                    List.of(),
                    List.of(),
                    new ConstraintsSummary(0, List.of()),
                    analysis,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    100,
                    100,
                    null,
                    null,
                    null,
                    null);
        }

        private AuditReport createReportWithTechnicalDebt(TechnicalDebtSummary debt) {
            return new AuditReport(
                    createDefaultMetadata(),
                    createPassingSummary(),
                    List.of(),
                    List.of(),
                    new ConstraintsSummary(0, List.of()),
                    null,
                    null,
                    null,
                    null,
                    debt,
                    null,
                    null,
                    100,
                    100,
                    null,
                    null,
                    null,
                    null);
        }

        private AuditReport createReportWithRecommendations(List<Recommendation> recommendations) {
            return new AuditReport(
                    createDefaultMetadata(),
                    createPassingSummary(),
                    List.of(),
                    List.of(),
                    new ConstraintsSummary(0, List.of()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    recommendations,
                    null,
                    100,
                    100,
                    null,
                    null,
                    null,
                    null);
        }

        private AuditReport createReportWithAnalysisAndDebt(ArchitectureAnalysis analysis, TechnicalDebtSummary debt) {
            return new AuditReport(
                    createDefaultMetadata(),
                    createPassingSummary(),
                    List.of(),
                    List.of(),
                    new ConstraintsSummary(0, List.of()),
                    analysis,
                    null,
                    null,
                    null,
                    debt,
                    null,
                    null,
                    100,
                    100,
                    null,
                    null,
                    null,
                    null);
        }

        private ReportMetadata createDefaultMetadata() {
            return new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-22T10:00:00Z"), "1.00s", "5.0.0");
        }

        private AuditSummary createPassingSummary() {
            return new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        }
    }
}
