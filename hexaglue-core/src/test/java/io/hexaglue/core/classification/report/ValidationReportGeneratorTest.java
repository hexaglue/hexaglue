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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.graph.model.NodeId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValidationReportGenerator}.
 */
class ValidationReportGeneratorTest {

    private ValidationReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ValidationReportGenerator();
    }

    @Nested
    @DisplayName("Console Report")
    class ConsoleReportTest {

        @Test
        @DisplayName("Should generate header with project name and timestamp")
        void shouldGenerateHeaderWithProjectInfo() {
            ClassificationResults results = emptyResults();

            String report = generator.generateConsole(results, "MyProject");

            assertThat(report)
                    .contains("HEXAGLUE VALIDATION REPORT")
                    .contains("Project: MyProject")
                    .contains("Generated:");
        }

        @Test
        @DisplayName("Should show PASSED status when no unclassified types")
        void shouldShowPassedWhenNoUnclassified() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(explicit);

            String report = generator.generateConsole(results, "Test");

            assertThat(report).contains("Status: PASSED");
        }

        @Test
        @DisplayName("Should show FAILED status when unclassified types exist")
        void shouldShowFailedWhenUnclassifiedExist() {
            ClassificationResult unclassified = createUnclassifiedResult("com.example.Unknown");
            ClassificationResults results = resultsOf(unclassified);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("Status: FAILED")
                    .contains("UNCLASSIFIED TYPES")
                    .contains("ACTION REQUIRED");
        }

        @Test
        @DisplayName("Should display classification summary with percentages")
        void shouldDisplaySummaryWithPercentages() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResult inferred = createInferredResult("com.example.OrderId", "VALUE_OBJECT");
            ClassificationResults results = resultsOf(explicit, inferred);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("CLASSIFICATION SUMMARY")
                    .contains("EXPLICIT:")
                    .contains("INFERRED:")
                    .contains("UNCLASSIFIED:")
                    .contains("TOTAL:");
        }

        @Test
        @DisplayName("Should list explicit classifications with annotations")
        void shouldListExplicitClassificationsWithAnnotations() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(explicit);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("EXPLICIT CLASSIFICATIONS")
                    .contains("Order")
                    .contains("AGGREGATE_ROOT");
        }

        @Test
        @DisplayName("Should list inferred classifications with evidence")
        void shouldListInferredClassificationsWithEvidence() {
            ClassificationResult inferred = createInferredResult("com.example.OrderId", "VALUE_OBJECT");
            ClassificationResults results = resultsOf(inferred);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("INFERRED CLASSIFICATIONS")
                    .contains("OrderId")
                    .contains("VALUE_OBJECT");
        }

        @Test
        @DisplayName("Should suggest actions for unclassified types")
        void shouldSuggestActionsForUnclassified() {
            ClassificationResult unclassified = createUnclassifiedResult("com.example.Ambiguous");
            ClassificationResults results = resultsOf(unclassified);

            String report = generator.generateConsole(results, "Test");

            assertThat(report).contains("Suggested Actions:").contains("exclude:");
        }
    }

    @Nested
    @DisplayName("Markdown Report")
    class MarkdownReportTest {

        @Test
        @DisplayName("Should generate proper markdown header")
        void shouldGenerateMarkdownHeader() {
            ClassificationResults results = emptyResults();

            String report = generator.generateMarkdown(results, "MyProject");

            assertThat(report)
                    .contains("# HexaGlue Validation Report")
                    .contains("**Generated:**")
                    .contains("**Project:** MyProject");
        }

        @Test
        @DisplayName("Should show checkmark for passed validation")
        void shouldShowCheckmarkForPassed() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(explicit);

            String report = generator.generateMarkdown(results, "Test");

            assertThat(report).contains("✅ PASSED");
        }

        @Test
        @DisplayName("Should show X for failed validation")
        void shouldShowXForFailed() {
            ClassificationResult unclassified = createUnclassifiedResult("com.example.Unknown");
            ClassificationResults results = resultsOf(unclassified);

            String report = generator.generateMarkdown(results, "Test");

            assertThat(report).contains("❌ FAILED");
        }

        @Test
        @DisplayName("Should generate markdown tables for classifications")
        void shouldGenerateMarkdownTables() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(explicit);

            String report = generator.generateMarkdown(results, "Test");

            assertThat(report).contains("| Category | Count | Percentage |").contains("| Type | Kind | Annotation |");
        }

        @Test
        @DisplayName("Should format types as code in markdown")
        void shouldFormatTypesAsCode() {
            ClassificationResult explicit = createExplicitResult("com.example.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(explicit);

            String report = generator.generateMarkdown(results, "Test");

            assertThat(report).contains("| `Order` |");
        }

        @Test
        @DisplayName("Should number unclassified types")
        void shouldNumberUnclassifiedTypes() {
            ClassificationResult unclassified1 = createUnclassifiedResult("com.example.First");
            ClassificationResult unclassified2 = createUnclassifiedResult("com.example.Second");
            ClassificationResults results = resultsOf(unclassified1, unclassified2);

            String report = generator.generateMarkdown(results, "Test");

            // Order is not guaranteed due to HashMap, but both should be present and numbered
            assertThat(report)
                    .contains("### 1.")
                    .contains("### 2.")
                    .contains("`com.example.First`")
                    .contains("`com.example.Second`");
        }
    }

    @Nested
    @DisplayName("Category Classification")
    class CategoryClassificationTest {

        @Test
        @DisplayName("Priority >= 100 should be categorized as EXPLICIT")
        void highPriorityShouldBeExplicit() {
            ClassificationResult result = ClassificationResult.classified(
                    NodeId.type("com.example.Order"),
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.HIGH,
                    "ExplicitConfiguration",
                    100,
                    "Explicit config",
                    List.of(),
                    List.of(),
                    null);
            ClassificationResults results = resultsOf(result);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("EXPLICIT CLASSIFICATIONS (1 types)")
                    .doesNotContain("INFERRED CLASSIFICATIONS");
        }

        @Test
        @DisplayName("Annotation evidence should be categorized as EXPLICIT")
        void annotationEvidenceShouldBeExplicit() {
            Evidence annotationEvidence =
                    new Evidence(EvidenceType.ANNOTATION, "@AggregateRoot annotation present", List.of());
            ClassificationResult result = ClassificationResult.classified(
                    NodeId.type("com.example.Order"),
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    ConfidenceLevel.HIGH,
                    "jMolecules",
                    95,
                    "jMolecules annotation",
                    List.of(annotationEvidence),
                    List.of(),
                    null);
            ClassificationResults results = resultsOf(result);

            String report = generator.generateConsole(results, "Test");

            assertThat(report).contains("EXPLICIT CLASSIFICATIONS (1 types)");
        }

        @Test
        @DisplayName("Priority < 100 without annotation should be INFERRED")
        void lowPriorityWithoutAnnotationShouldBeInferred() {
            Evidence structureEvidence = new Evidence(EvidenceType.STRUCTURE, "Is a Java record", List.of());
            ClassificationResult result = ClassificationResult.classified(
                    NodeId.type("com.example.OrderId"),
                    ClassificationTarget.DOMAIN,
                    "VALUE_OBJECT",
                    ConfidenceLevel.MEDIUM,
                    "RecordCriteria",
                    80,
                    "Java record with Id suffix",
                    List.of(structureEvidence),
                    List.of(),
                    null);
            ClassificationResults results = resultsOf(result);

            String report = generator.generateConsole(results, "Test");

            assertThat(report)
                    .contains("INFERRED CLASSIFICATIONS (1 types)")
                    .doesNotContain("EXPLICIT CLASSIFICATIONS");
        }

        @Test
        @DisplayName("UNCLASSIFIED status should be categorized as UNCLASSIFIED")
        void unclassifiedStatusShouldBeUnclassified() {
            ClassificationResult unclassified = createUnclassifiedResult("com.example.Unknown");
            ClassificationResults results = resultsOf(unclassified);

            String report = generator.generateConsole(results, "Test");

            assertThat(report).contains("UNCLASSIFIED TYPES (1 types)").contains("ACTION REQUIRED");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("Should handle empty results")
        void shouldHandleEmptyResults() {
            ClassificationResults results = emptyResults();

            String consoleReport = generator.generateConsole(results, "Test");
            String markdownReport = generator.generateMarkdown(results, "Test");

            assertThat(consoleReport).contains("TOTAL:").contains("0").contains("Status: PASSED");
            assertThat(markdownReport).contains("**Total** | **0**").contains("✅ PASSED");
        }

        @Test
        @DisplayName("Should handle null project name")
        void shouldHandleNullProjectName() {
            ClassificationResults results = emptyResults();

            String report = generator.generateConsole(results, null);

            assertThat(report).contains("Project: Unknown");
        }

        @Test
        @DisplayName("Should extract simple name from qualified name")
        void shouldExtractSimpleName() {
            ClassificationResult result = createExplicitResult("com.example.domain.model.Order", "AGGREGATE_ROOT");
            ClassificationResults results = resultsOf(result);

            String report = generator.generateConsole(results, "Test");

            // Should show simple name in explicit section
            assertThat(report).contains("Order");
        }

        @Test
        @DisplayName("Should show full qualified name for unclassified")
        void shouldShowQualifiedNameForUnclassified() {
            ClassificationResult unclassified = createUnclassifiedResult("com.example.domain.model.Ambiguous");
            ClassificationResults results = resultsOf(unclassified);

            String report = generator.generateConsole(results, "Test");

            // Unclassified should show full qualified name
            assertThat(report).contains("com.example.domain.model.Ambiguous");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private ClassificationResults emptyResults() {
        return new ClassificationResults(Map.of());
    }

    private ClassificationResults resultsOf(ClassificationResult... results) {
        Map<NodeId, ClassificationResult> map = new HashMap<>();
        for (ClassificationResult r : results) {
            map.put(r.subjectId(), r);
        }
        return new ClassificationResults(map);
    }

    private ClassificationResult createExplicitResult(String fqn, String kind) {
        Evidence annotationEvidence = new Evidence(EvidenceType.ANNOTATION, "@" + kind + " annotation", List.of());
        return ClassificationResult.classified(
                NodeId.type(fqn),
                ClassificationTarget.DOMAIN,
                kind,
                ConfidenceLevel.HIGH,
                "jMolecules",
                100,
                "jMolecules annotation present",
                List.of(annotationEvidence),
                List.of(),
                null);
    }

    private ClassificationResult createInferredResult(String fqn, String kind) {
        Evidence structureEvidence = new Evidence(EvidenceType.STRUCTURE, "Structural analysis", List.of());
        return ClassificationResult.classified(
                NodeId.type(fqn),
                ClassificationTarget.DOMAIN,
                kind,
                ConfidenceLevel.MEDIUM,
                "StructureCriteria",
                80,
                "Inferred from structure",
                List.of(structureEvidence),
                List.of(),
                null);
    }

    private ClassificationResult createUnclassifiedResult(String fqn) {
        return ClassificationResult.unclassified(NodeId.type(fqn));
    }
}
