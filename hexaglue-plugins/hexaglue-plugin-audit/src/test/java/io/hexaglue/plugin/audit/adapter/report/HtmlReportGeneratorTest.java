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

class HtmlReportGeneratorTest {

    @Test
    void shouldGenerateValidHtmlStructure() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        HtmlReportGenerator generator = new HtmlReportGenerator();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("<html lang=\"en\">");
        assertThat(html).contains("</html>");
        assertThat(html).contains("<head>");
        assertThat(html).contains("<body>");
        assertThat(html).contains("<style>");
    }

    @Test
    void shouldDisplayPassedStatus() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        HtmlReportGenerator generator = new HtmlReportGenerator();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("PASSED");
        assertThat(html).contains("class=\"card pass\"");
    }

    @Test
    void shouldDisplayFailedStatus() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry("ddd:test", "BLOCKER", "Test violation", "Type", "File.java:1:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        HtmlReportGenerator generator = new HtmlReportGenerator();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("FAILED");
        assertThat(html).contains("class=\"card fail\"");
    }

    @Test
    void shouldRenderViolationsTable() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry(
                "ddd:entity-identity", "BLOCKER", "Entity must have ID", "Order", "Order.java:10:5", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        HtmlReportGenerator generator = new HtmlReportGenerator();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("<table>");
        assertThat(html).contains("ddd:entity-identity");
        assertThat(html).contains("Entity must have ID");
        assertThat(html).contains("Order");
        assertThat(html).contains("badge-blocker");
    }

    @Test
    void shouldEscapeHtmlSpecialCharacters() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test<script>", "1.0.0", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 0, 0, 1, 0, 0);
        ViolationEntry v = new ViolationEntry(
                "test:xss", "MAJOR", "Message with <script>alert('xss')</script>", "Type&Name", "File.java:1:1", "");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        HtmlReportGenerator generator = new HtmlReportGenerator();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&amp;");
        assertThat(html).doesNotContain("<script>alert");
    }

    @Test
    void shouldReturnCorrectFormat() {
        // Given
        HtmlReportGenerator generator = new HtmlReportGenerator();

        // Then
        assertThat(generator.format()).isEqualTo(ReportFormat.HTML);
    }
}
