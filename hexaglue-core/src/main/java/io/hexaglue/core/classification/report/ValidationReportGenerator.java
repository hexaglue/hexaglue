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

package io.hexaglue.core.classification.report;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.graph.model.NodeId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates validation reports showing the classification state of all types.
 *
 * <p>The report distinguishes between:
 * <ul>
 *   <li><b>EXPLICIT</b>: Types with explicit annotations or user configuration (priority 100)</li>
 *   <li><b>INFERRED</b>: Types classified by strong semantic signals (priority &lt; 100)</li>
 *   <li><b>UNCLASSIFIED</b>: Ambiguous types requiring user intervention</li>
 * </ul>
 *
 * <p>For unclassified types, the report provides suggested actions:
 * <ul>
 *   <li>Add appropriate jMolecules annotation</li>
 *   <li>Configure explicit classification in hexaglue.yaml</li>
 *   <li>Add to exclusion patterns if not a domain type</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class ValidationReportGenerator {

    private static final int EXPLICIT_PRIORITY_THRESHOLD = 100;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Classification category for report grouping.
     */
    public enum Category {
        EXPLICIT,
        INFERRED,
        UNCLASSIFIED
    }

    /**
     * Generates a console report suitable for terminal/CI logs.
     *
     * @param results the classification results
     * @param projectName the project name
     * @return formatted console report
     */
    public String generateConsole(ClassificationResults results, String projectName) {
        StringBuilder report = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Categorize results
        Map<Category, List<ClassificationResult>> categorized = categorizeResults(results);
        List<ClassificationResult> explicit = categorized.getOrDefault(Category.EXPLICIT, List.of());
        List<ClassificationResult> inferred = categorized.getOrDefault(Category.INFERRED, List.of());
        List<ClassificationResult> unclassified = categorized.getOrDefault(Category.UNCLASSIFIED, List.of());

        int total = explicit.size() + inferred.size() + unclassified.size();
        boolean passed = unclassified.isEmpty();

        // Header
        report.append("=".repeat(80)).append("\n");
        report.append("HEXAGLUE VALIDATION REPORT\n");
        report.append("=".repeat(80)).append("\n");
        report.append("Generated: ").append(timestamp).append("\n");
        report.append("Project: ")
                .append(projectName != null ? projectName : "Unknown")
                .append("\n");
        report.append("\n");

        // Summary
        report.append("CLASSIFICATION SUMMARY\n");
        report.append("-".repeat(80)).append("\n");
        report.append(String.format(
                "%-20s %5d (%5.1f%%)\n", "EXPLICIT:", explicit.size(), percentage(explicit.size(), total)));
        report.append(String.format(
                "%-20s %5d (%5.1f%%)\n", "INFERRED:", inferred.size(), percentage(inferred.size(), total)));
        report.append(String.format(
                "%-20s %5d (%5.1f%%)\n", "UNCLASSIFIED:", unclassified.size(), percentage(unclassified.size(), total)));
        report.append(String.format("%-20s %5d\n", "TOTAL:", total));
        report.append("\n");
        report.append("Status: ").append(passed ? "PASSED" : "FAILED").append("\n");
        report.append("\n");

        // Explicit Classifications
        if (!explicit.isEmpty()) {
            report.append("EXPLICIT CLASSIFICATIONS (").append(explicit.size()).append(" types)\n");
            report.append("-".repeat(80)).append("\n");
            for (ClassificationResult r : explicit) {
                String typeName = extractSimpleName(r.subjectId());
                String annotation = extractAnnotation(r);
                report.append(String.format("  %-40s %-20s %s\n", typeName, r.kind(), annotation));
            }
            report.append("\n");
        }

        // Inferred Classifications
        if (!inferred.isEmpty()) {
            report.append("INFERRED CLASSIFICATIONS (").append(inferred.size()).append(" types)\n");
            report.append("-".repeat(80)).append("\n");
            for (ClassificationResult r : inferred) {
                String typeName = extractSimpleName(r.subjectId());
                String evidence = summarizeEvidence(r);
                report.append(String.format("  %-40s %-20s %s\n", typeName, r.kind(), evidence));
            }
            report.append("\n");
        }

        // Unclassified Types
        if (!unclassified.isEmpty()) {
            report.append("UNCLASSIFIED TYPES (").append(unclassified.size()).append(" types) - ACTION REQUIRED\n");
            report.append("-".repeat(80)).append("\n");
            for (ClassificationResult r : unclassified) {
                String typeName = extractQualifiedName(r.subjectId());
                report.append("\n  ").append(typeName).append("\n");
                report.append("    Reason: ")
                        .append(r.justification() != null ? r.justification() : "No matching criteria")
                        .append("\n");
                report.append("    Suggested Actions:\n");
                for (String action : suggestActions(r)) {
                    report.append("      - ").append(action).append("\n");
                }
            }
            report.append("\n");
        }

        report.append("=".repeat(80)).append("\n");
        if (!passed) {
            report.append(unclassified.size()).append(" unclassified types must be resolved before generation.\n");
        }

        return report.toString();
    }

    /**
     * Generates a Markdown report suitable for documentation.
     *
     * @param results the classification results
     * @param projectName the project name
     * @return formatted Markdown report
     */
    public String generateMarkdown(ClassificationResults results, String projectName) {
        StringBuilder md = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // Categorize results
        Map<Category, List<ClassificationResult>> categorized = categorizeResults(results);
        List<ClassificationResult> explicit = categorized.getOrDefault(Category.EXPLICIT, List.of());
        List<ClassificationResult> inferred = categorized.getOrDefault(Category.INFERRED, List.of());
        List<ClassificationResult> unclassified = categorized.getOrDefault(Category.UNCLASSIFIED, List.of());

        int total = explicit.size() + inferred.size() + unclassified.size();
        boolean passed = unclassified.isEmpty();

        // Header
        md.append("# HexaGlue Validation Report\n\n");
        md.append("**Generated:** ").append(timestamp).append("  \n");
        md.append("**Project:** ")
                .append(projectName != null ? projectName : "Unknown")
                .append("  \n");
        md.append("**Status:** ");
        if (passed) {
            md.append("✅ PASSED\n\n");
        } else {
            md.append("❌ FAILED\n\n");
        }

        // Summary
        md.append("## Classification Summary\n\n");
        md.append("| Category | Count | Percentage |\n");
        md.append("|----------|-------|------------|\n");
        md.append(String.format("| EXPLICIT | %d | %.1f%% |\n", explicit.size(), percentage(explicit.size(), total)));
        md.append(String.format("| INFERRED | %d | %.1f%% |\n", inferred.size(), percentage(inferred.size(), total)));
        md.append(String.format(
                "| UNCLASSIFIED | %d | %.1f%% |\n", unclassified.size(), percentage(unclassified.size(), total)));
        md.append(String.format("| **Total** | **%d** | 100%% |\n\n", total));

        // Explicit Classifications
        if (!explicit.isEmpty()) {
            md.append("## Explicit Classifications (").append(explicit.size()).append(" types)\n\n");
            md.append("| Type | Kind | Annotation |\n");
            md.append("|------|------|------------|\n");
            for (ClassificationResult r : explicit) {
                String typeName = extractSimpleName(r.subjectId());
                String annotation = extractAnnotation(r);
                md.append(String.format("| `%s` | %s | %s |\n", typeName, r.kind(), annotation));
            }
            md.append("\n");
        }

        // Inferred Classifications
        if (!inferred.isEmpty()) {
            md.append("## Inferred Classifications (").append(inferred.size()).append(" types)\n\n");
            md.append("| Type | Kind | Evidence |\n");
            md.append("|------|------|----------|\n");
            for (ClassificationResult r : inferred) {
                String typeName = extractSimpleName(r.subjectId());
                String evidence = summarizeEvidence(r);
                md.append(String.format("| `%s` | %s | %s |\n", typeName, r.kind(), evidence));
            }
            md.append("\n");
        }

        // Unclassified Types
        if (!unclassified.isEmpty()) {
            md.append("## Unclassified Types (").append(unclassified.size()).append(" types) - ACTION REQUIRED\n\n");

            int index = 1;
            for (ClassificationResult r : unclassified) {
                String typeName = extractQualifiedName(r.subjectId());
                md.append("### ").append(index++).append(". `").append(typeName).append("`\n\n");
                md.append("**Reason:** ")
                        .append(r.justification() != null ? r.justification() : "No matching criteria")
                        .append("\n\n");
                md.append("**Suggested Actions:**\n");
                for (String action : suggestActions(r)) {
                    md.append("- ").append(action).append("\n");
                }
                md.append("\n");
            }
        }

        // Footer
        md.append("---\n\n");
        if (!passed) {
            md.append("## Validation Status: FAILED\n\n");
            md.append(unclassified.size()).append(" unclassified types must be resolved before generation.\n");
        } else {
            md.append("## Validation Status: PASSED\n\n");
            md.append("All types are classified. Generation can proceed.\n");
        }

        return md.toString();
    }

    /**
     * Categorizes classification results into EXPLICIT, INFERRED, and UNCLASSIFIED.
     */
    private Map<Category, List<ClassificationResult>> categorizeResults(ClassificationResults results) {
        return results.stream().collect(Collectors.groupingBy(this::categorize));
    }

    /**
     * Determines the category of a classification result.
     */
    private Category categorize(ClassificationResult result) {
        if (result.status() == ClassificationStatus.UNCLASSIFIED) {
            return Category.UNCLASSIFIED;
        }

        // Check for explicit classification (priority 100 or annotation evidence)
        if (result.matchedPriority() >= EXPLICIT_PRIORITY_THRESHOLD) {
            return Category.EXPLICIT;
        }

        // Check for annotation-based evidence
        if (result.evidence() != null
                && result.evidence().stream().anyMatch(e -> e.type() == EvidenceType.ANNOTATION)) {
            return Category.EXPLICIT;
        }

        return Category.INFERRED;
    }

    /**
     * Extracts simple type name from NodeId.
     */
    private String extractSimpleName(NodeId nodeId) {
        String value = nodeId.value();
        // Format: "type:com.example.Order"
        if (value.startsWith("type:")) {
            String fqn = value.substring(5);
            int lastDot = fqn.lastIndexOf('.');
            return lastDot > 0 ? fqn.substring(lastDot + 1) : fqn;
        }
        return value;
    }

    /**
     * Extracts qualified type name from NodeId.
     */
    private String extractQualifiedName(NodeId nodeId) {
        String value = nodeId.value();
        // Format: "type:com.example.Order"
        if (value.startsWith("type:")) {
            return value.substring(5);
        }
        return value;
    }

    /**
     * Extracts annotation name from evidence.
     */
    private String extractAnnotation(ClassificationResult result) {
        if (result.evidence() == null || result.evidence().isEmpty()) {
            if (result.matchedCriteria() != null && result.matchedCriteria().equals("ExplicitConfiguration")) {
                return "hexaglue.yaml";
            }
            return "-";
        }

        return result.evidence().stream()
                .filter(e -> e.type() == EvidenceType.ANNOTATION)
                .map(Evidence::description)
                .findFirst()
                .orElse(
                        result.matchedCriteria() != null
                                        && result.matchedCriteria().equals("ExplicitConfiguration")
                                ? "hexaglue.yaml"
                                : "-");
    }

    /**
     * Summarizes evidence for display.
     */
    private String summarizeEvidence(ClassificationResult result) {
        if (result.evidence() == null || result.evidence().isEmpty()) {
            return result.matchedCriteria() != null ? result.matchedCriteria() : "-";
        }

        // Take the first non-annotation evidence as summary
        return result.evidence().stream()
                .filter(e -> e.type() != EvidenceType.ANNOTATION)
                .map(Evidence::description)
                .findFirst()
                .orElse(result.matchedCriteria() != null ? result.matchedCriteria() : "-");
    }

    /**
     * Suggests actions to resolve an unclassified type.
     */
    private List<String> suggestActions(ClassificationResult result) {
        List<String> actions = new ArrayList<>();
        String typeName = extractQualifiedName(result.subjectId());
        String simpleName = extractSimpleName(result.subjectId());

        // Suggest based on target type
        if (result.target() == ClassificationTarget.DOMAIN) {
            actions.add("Add `@AggregateRoot`, `@Entity`, or `@ValueObject` annotation from jMolecules");
            actions.add("Configure in hexaglue.yaml: `explicit: { " + typeName + ": ENTITY }`");
        } else if (result.target() == ClassificationTarget.PORT) {
            actions.add("Add `@Repository`, `@PrimaryPort`, or `@SecondaryPort` annotation from jMolecules");
            actions.add("Configure in hexaglue.yaml: `explicit: { " + typeName + ": REPOSITORY }`");
        }

        // Always suggest exclusion as an option
        actions.add("Exclude from classification: `exclude: [\"**." + simpleName + "\"]`");

        return actions;
    }

    /**
     * Calculates percentage safely.
     */
    private double percentage(int count, int total) {
        return total == 0 ? 0.0 : (count * 100.0 / total);
    }
}
