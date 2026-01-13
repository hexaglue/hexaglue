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

package io.hexaglue.core.audit.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.audit.*;
import io.hexaglue.spi.core.SourceLocation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuditReportGenerator}.
 */
class AuditReportGeneratorTest {

    private AuditReportGenerator generator;
    private AuditSnapshot snapshot;

    @BeforeEach
    void setUp() {
        generator = new AuditReportGenerator();
        snapshot = createSampleSnapshot();
    }

    @Test
    @DisplayName("should generate console report with header")
    void shouldGenerateConsoleReportWithHeader() {
        String report = generator.generateConsole(snapshot);

        assertThat(report).contains("HEXAGLUE AUDIT REPORT");
        assertThat(report).contains("Project: TestProject");
        assertThat(report).contains("HexaGlue Version: 3.0.0");
    }

    @Test
    @DisplayName("should include summary in console report")
    void shouldIncludeSummaryInConsoleReport() {
        String report = generator.generateConsole(snapshot);

        assertThat(report).contains("SUMMARY");
        assertThat(report).contains("Total Violations:");
        assertThat(report).contains("Errors:");
        assertThat(report).contains("Warnings:");
    }

    @Test
    @DisplayName("should include quality metrics in console report")
    void shouldIncludeQualityMetricsInConsoleReport() {
        String report = generator.generateConsole(snapshot);

        assertThat(report).contains("QUALITY METRICS");
        assertThat(report).contains("Test Coverage:");
        assertThat(report).contains("Documentation Coverage:");
        assertThat(report).contains("Technical Debt:");
        assertThat(report).contains("Maintainability Rating:");
    }

    @Test
    @DisplayName("should include violations in console report")
    void shouldIncludeViolationsInConsoleReport() {
        String report = generator.generateConsole(snapshot);

        assertThat(report).contains("VIOLATIONS");
        assertThat(report).contains("hexaglue.test.rule");
        assertThat(report).contains("Test violation message");
    }

    @Test
    @DisplayName("should generate valid HTML report")
    void shouldGenerateValidHtmlReport() {
        String report = generator.generateHtml(snapshot);

        assertThat(report).contains("<!DOCTYPE html>");
        assertThat(report).contains("<html lang=\"en\">");
        assertThat(report).contains("<title>HexaGlue Audit Report</title>");
        assertThat(report).contains("</html>");
    }

    @Test
    @DisplayName("should include CSS styles in HTML report")
    void shouldIncludeCssInHtmlReport() {
        String report = generator.generateHtml(snapshot);

        assertThat(report).contains("<style>");
        assertThat(report).contains("font-family:");
        assertThat(report).contains("</style>");
    }

    @Test
    @DisplayName("should generate valid JSON report")
    void shouldGenerateValidJsonReport() {
        String report = generator.generateJson(snapshot);

        assertThat(report).startsWith("{");
        assertThat(report).endsWith("}\n");
        assertThat(report).contains("\"metadata\":");
        assertThat(report).contains("\"summary\":");
        assertThat(report).contains("\"qualityMetrics\":");
        assertThat(report).contains("\"violations\":");
    }

    @Test
    @DisplayName("should include all fields in JSON report")
    void shouldIncludeAllFieldsInJsonReport() {
        String report = generator.generateJson(snapshot);

        assertThat(report).contains("\"projectName\": \"TestProject\"");
        assertThat(report).contains("\"hexaglueVersion\": \"3.0.0\"");
        assertThat(report).contains("\"passed\":");
        assertThat(report).contains("\"testCoverage\":");
    }

    @Test
    @DisplayName("should generate Markdown report with header")
    void shouldGenerateMarkdownReportWithHeader() {
        String report = generator.generateMarkdown(snapshot);

        assertThat(report).contains("# HexaGlue Audit Report");
        assertThat(report).contains("**Generated:**");
        assertThat(report).contains("**Project:** TestProject");
        assertThat(report).contains("**Status:**");
    }

    @Test
    @DisplayName("should include tables in Markdown report")
    void shouldIncludeTablesInMarkdownReport() {
        String report = generator.generateMarkdown(snapshot);

        assertThat(report).contains("## Summary");
        assertThat(report).contains("| Metric | Count |");
        assertThat(report).contains("## Quality Metrics");
        assertThat(report).contains("| Metric | Value |");
    }

    @Test
    @DisplayName("should include violations in Markdown report")
    void shouldIncludeViolationsInMarkdownReport() {
        String report = generator.generateMarkdown(snapshot);

        assertThat(report).contains("## Violations");
        assertThat(report).contains("hexaglue.test.rule");
        assertThat(report).contains("Test violation message");
    }

    @Test
    @DisplayName("should show passed status for snapshot with no errors")
    void shouldShowPassedStatus() {
        AuditSnapshot passedSnapshot = new AuditSnapshot(
                createCodebase(),
                DetectedArchitectureStyle.HEXAGONAL,
                List.of(),
                createQualityMetrics(),
                createArchitectureMetrics(),
                createMetadata());

        String consoleReport = generator.generateConsole(passedSnapshot);
        String markdownReport = generator.generateMarkdown(passedSnapshot);

        assertThat(consoleReport).contains("Status: PASSED");
        assertThat(markdownReport).contains("✅ PASSED");
    }

    // Helper methods

    private AuditSnapshot createSampleSnapshot() {
        RuleViolation violation = new RuleViolation(
                "hexaglue.test.rule",
                Severity.WARNING,
                "Test violation message",
                SourceLocation.of("Test.java", 10, 5));

        return new AuditSnapshot(
                createCodebase(),
                DetectedArchitectureStyle.HEXAGONAL,
                List.of(violation),
                createQualityMetrics(),
                createArchitectureMetrics(),
                createMetadata());
    }

    private Codebase createCodebase() {
        return new Codebase("TestProject", "com.example", List.of(), java.util.Map.of());
    }

    private QualityMetrics createQualityMetrics() {
        return new QualityMetrics(85.0, 75.0, 120, 4.2);
    }

    private ArchitectureMetrics createArchitectureMetrics() {
        return new ArchitectureMetrics(10, 0.3, 0.8, 0);
    }

    private AuditMetadata createMetadata() {
        return new AuditMetadata(java.time.Instant.now(), "3.0.0", java.time.Duration.ofSeconds(5));
    }
}
