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

import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.AuditSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ConstraintsSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ReportMetadata;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownReportGeneratorTest {

    @Test
    void shouldGenerateMarkdownWithTitle() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("# HexaGlue Audit Report");
    }

    @Test
    void shouldShowPassedStatusWithEmoji() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("✅ PASSED");
        assertThat(markdown).contains("✅ **No violations found.**");
    }

    @Test
    void shouldShowFailedStatusWithEmoji() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry("ddd:test", "BLOCKER", "Test violation", "Type", "File.java:1:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("❌ FAILED");
    }

    @Test
    void shouldGenerateSummaryTable() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 5, 1, 1, 2, 1, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## Summary");
        assertThat(markdown).contains("| Total Violations | 5 |");
        assertThat(markdown).contains("| Blockers | 1 |");
        assertThat(markdown).contains("| Majors | 2 |");
    }

    @Test
    void shouldGenerateViolationsTable() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry(
                "ddd:entity-identity", "BLOCKER", "Entity must have ID", "Order", "Order.java:10:5", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## Violations");
        assertThat(markdown).contains("| Severity | Constraint | Message | Affected Type | Location |");
        assertThat(markdown).contains("ddd:entity-identity");
        assertThat(markdown).contains("Entity must have ID");
        assertThat(markdown).contains("`Order`");
    }

    @Test
    void shouldUseCollapsibleSections() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry("ddd:test", "BLOCKER", "Test violation", "Type", "File.java:1:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("<details");
        assertThat(markdown).contains("<summary>");
        assertThat(markdown).contains("</details>");
    }

    @Test
    void shouldEscapeMarkdownSpecialCharacters() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 0, 0, 1, 0, 0);
        ViolationEntry v =
                new ViolationEntry("test:pipe", "MAJOR", "Message with | pipe character", "Type", "File.java:1:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("\\|");
    }

    @Test
    void shouldReturnCorrectFormat() {
        // Given
        MarkdownReportGenerator generator = new MarkdownReportGenerator();

        // Then
        assertThat(generator.format()).isEqualTo(ReportFormat.MARKDOWN);
    }
}
