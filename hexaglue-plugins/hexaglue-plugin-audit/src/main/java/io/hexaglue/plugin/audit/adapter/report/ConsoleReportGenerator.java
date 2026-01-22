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

import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Generates audit reports for console output with ANSI colors.
 *
 * <p>This generator produces terminal-friendly output with:
 * <ul>
 *   <li>ANSI color codes for severity levels</li>
 *   <li>Box-drawing characters for tables</li>
 *   <li>Clear section separators</li>
 *   <li>Fallback to plain text when colors are not supported</li>
 * </ul>
 *
 * <p>The generator automatically detects terminal support and falls back
 * to plain text output when ANSI codes are not available.
 *
 * @since 1.0.0
 */
public final class ConsoleReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";

    // Box drawing characters
    private static final String BOX_HORIZONTAL = "─";
    private static final String BOX_VERTICAL = "│";

    private final boolean useColors;

    /**
     * Creates a console report generator with color support detection.
     */
    public ConsoleReportGenerator() {
        this(detectColorSupport());
    }

    /**
     * Creates a console report generator with explicit color support setting.
     *
     * @param useColors whether to use ANSI colors
     */
    public ConsoleReportGenerator(boolean useColors) {
        this.useColors = useColors;
    }

    @Override
    public ReportFormat format() {
        return ReportFormat.CONSOLE;
    }

    @Override
    public String generate(AuditReport report) {
        Objects.requireNonNull(report, "report required");

        StringBuilder console = new StringBuilder();

        // Header
        console.append("\n");
        console.append(boxLine(80));
        console.append(boxRow("HexaGlue Audit Report", 80, true));
        console.append(boxLine(80));
        console.append("\n");

        // Metadata
        console.append(colorize(CYAN, "Project:         "))
                .append(report.metadata().projectName())
                .append("\n");
        console.append(colorize(CYAN, "Timestamp:       "))
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("\n");
        console.append(colorize(CYAN, "Duration:        "))
                .append(report.metadata().duration())
                .append("\n");
        console.append(colorize(CYAN, "HexaGlue Version:"))
                .append(" ")
                .append(report.metadata().hexaglueVersion())
                .append("\n\n");

        // Summary
        String statusColor = report.summary().passed() ? GREEN : RED;
        String status = report.summary().passed() ? "PASSED" : "FAILED";
        console.append(colorize(BOLD + statusColor, "Status: " + status)).append("\n\n");

        console.append(colorize(BOLD, "Summary:")).append("\n");
        console.append("  Total Violations: ")
                .append(report.summary().totalViolations())
                .append("\n");
        console.append("  ")
                .append(colorize(RED, "Blockers:  " + report.summary().blockers()))
                .append("\n");
        console.append("  ")
                .append(colorize(RED, "Criticals: " + report.summary().criticals()))
                .append("\n");
        console.append("  ")
                .append(colorize(YELLOW, "Majors:    " + report.summary().majors()))
                .append("\n");
        console.append("  ")
                .append(colorize(BLUE, "Minors:    " + report.summary().minors()))
                .append("\n");
        console.append("  ")
                .append(colorize(GRAY, "Infos:     " + report.summary().infos()))
                .append("\n\n");

        // Violations
        console.append(separator(80));
        console.append(colorize(BOLD, "VIOLATIONS")).append("\n");
        console.append(separator(80));

        if (report.violations().isEmpty()) {
            console.append(colorize(GREEN, "No violations found.")).append("\n");
        } else {
            for (int i = 0; i < report.violations().size(); i++) {
                ViolationEntry v = report.violations().get(i);
                console.append("\n");
                console.append(colorize(BOLD, (i + 1) + ". " + v.severity()))
                        .append(" - ")
                        .append(colorize(GRAY, v.constraintId()))
                        .append("\n");
                console.append("   Message:       ").append(v.message()).append("\n");
                console.append("   Affected Type: ").append(v.affectedType()).append("\n");
                console.append("   Location:      ")
                        .append(colorize(GRAY, v.location()))
                        .append("\n");
            }
        }

        // Metrics
        if (!report.metrics().isEmpty()) {
            console.append("\n");
            console.append(separator(80));
            console.append(colorize(BOLD, "METRICS")).append("\n");
            console.append(separator(80));

            for (MetricEntry m : report.metrics()) {
                console.append("\n");
                console.append(colorize(CYAN, m.name())).append("\n");
                console.append("  Value:     ")
                        .append(colorize(BOLD, String.format(Locale.US, "%.2f", m.value())))
                        .append(" ")
                        .append(m.unit())
                        .append("\n");

                if (m.threshold() != null) {
                    console.append("  Threshold: ")
                            .append(m.thresholdType())
                            .append(" ")
                            .append(String.format(Locale.US, "%.2f", m.threshold()))
                            .append("\n");
                }

                String statusColor2 =
                        switch (m.status()) {
                            case "OK" -> GREEN;
                            case "WARNING" -> YELLOW;
                            case "CRITICAL" -> RED;
                            default -> RESET;
                        };
                console.append("  Status:    ")
                        .append(colorize(statusColor2, m.status()))
                        .append("\n");
            }
        }

        // Footer
        console.append("\n");
        console.append(boxLine(80));
        console.append("\n");

        return console.toString();
    }

    /**
     * Detects whether the terminal supports ANSI colors.
     */
    private static boolean detectColorSupport() {
        // Check for common CI environments that support colors
        String ci = System.getenv("CI");
        String term = System.getenv("TERM");

        // Enable colors if TERM suggests color support or in CI with color support
        return (term != null && (term.contains("color") || term.contains("256")))
                || ("true".equals(ci) && System.getenv("GITHUB_ACTIONS") != null);
    }

    /**
     * Colorizes text with ANSI codes if color support is enabled.
     */
    private String colorize(String color, String text) {
        if (!useColors) {
            return text;
        }
        return color + text + RESET;
    }

    /**
     * Creates a horizontal line.
     */
    private String boxLine(int width) {
        return BOX_HORIZONTAL.repeat(width) + "\n";
    }

    /**
     * Creates a box row with centered text.
     */
    private String boxRow(String text, int width, boolean bold) {
        int padding = (width - text.length() - 2) / 2;
        String padStr = " ".repeat(Math.max(0, padding));
        String content = padStr + text + padStr;
        if (content.length() < width - 2) {
            content += " ";
        }

        if (bold) {
            return BOX_VERTICAL + colorize(BOLD, content) + BOX_VERTICAL + "\n";
        }
        return BOX_VERTICAL + content + BOX_VERTICAL + "\n";
    }

    /**
     * Creates a separator line.
     */
    private String separator(int width) {
        return "=".repeat(width) + "\n";
    }
}
