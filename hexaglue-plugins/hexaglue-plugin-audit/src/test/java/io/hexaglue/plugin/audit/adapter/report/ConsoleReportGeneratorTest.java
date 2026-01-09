/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.AuditSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ConstraintsSummary;
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ReportMetadata;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleReportGeneratorTest {

    @Test
    void shouldGenerateConsoleReportWithoutColors() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("HexaGlue Audit Report");
        assertThat(console).contains("test-project");
        assertThat(console).contains("PASSED");
        assertThat(console).doesNotContain("\u001B["); // No ANSI codes
    }

    @Test
    void shouldGenerateConsoleReportWithColors() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(true);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("\u001B["); // Has ANSI codes
    }

    @Test
    void shouldDisplayPassedStatus() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("Status: PASSED");
    }

    @Test
    void shouldDisplayFailedStatus() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v =
                new ViolationEntry("ddd:test", "BLOCKER", "Test violation", "Type", "File.java:1:1", "");
        AuditReport report = new AuditReport(
                metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("Status: FAILED");
    }

    @Test
    void shouldDisplayViolationsSummary() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 5, 1, 1, 2, 1, 0);
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("Total Violations: 5");
        assertThat(console).contains("Blockers:  1");
        assertThat(console).contains("Majors:    2");
    }

    @Test
    void shouldDisplayViolationDetails() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(false, 1, 1, 0, 0, 0, 0);
        ViolationEntry v = new ViolationEntry(
                "ddd:entity-identity", "BLOCKER", "Entity must have ID", "Order", "Order.java:10:5", "");
        AuditReport report = new AuditReport(
                metadata, summary, List.of(v), List.of(), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("VIOLATIONS");
        assertThat(console).contains("BLOCKER");
        assertThat(console).contains("ddd:entity-identity");
        assertThat(console).contains("Entity must have ID");
        assertThat(console).contains("Order");
    }

    @Test
    void shouldDisplayMetrics() {
        // Given
        ReportMetadata metadata =
                new ReportMetadata("test-project", Instant.parse("2026-01-09T10:00:00Z"), "1.23s", "3.0.0");
        AuditSummary summary = new AuditSummary(true, 0, 0, 0, 0, 0, 0);
        MetricEntry metric = new MetricEntry("aggregate.avgSize", 15.5, "methods", 20.0, "max", "OK");
        AuditReport report =
                new AuditReport(metadata, summary, List.of(), List.of(metric), new ConstraintsSummary(0, List.of()));

        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // When
        String console = generator.generate(report);

        // Then
        assertThat(console).contains("METRICS");
        assertThat(console).contains("aggregate.avgSize");
        assertThat(console).contains("15.50");
        assertThat(console).contains("methods");
        assertThat(console).contains("Status:    OK");
    }

    @Test
    void shouldReturnCorrectFormat() {
        // Given
        ConsoleReportGenerator generator = new ConsoleReportGenerator(false);

        // Then
        assertThat(generator.format()).isEqualTo(ReportFormat.CONSOLE);
    }
}
