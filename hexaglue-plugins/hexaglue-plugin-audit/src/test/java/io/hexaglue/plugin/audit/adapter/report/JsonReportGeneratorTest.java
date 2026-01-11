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
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ReportMetadata;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
