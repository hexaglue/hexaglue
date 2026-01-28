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

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.report.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for parity between HTML and Markdown renderers.
 *
 * <p>Ensures both renderers produce equivalent content with the same data,
 * scores, grades, and diagrams (with appropriate encoding differences).
 *
 * @since 5.0.0
 */
class RenderersParityTest {

    private HtmlRenderer htmlRenderer;
    private MarkdownRenderer markdownRenderer;
    private ReportData testData;
    private DiagramSet diagrams;

    @BeforeEach
    void setUp() {
        htmlRenderer = new HtmlRenderer();
        markdownRenderer = new MarkdownRenderer();
        testData = createTestReportData();
        diagrams = createTestDiagramSet();
    }

    @Nested
    @DisplayName("Score and Grade Parity")
    class ScoreGradeParity {

        @Test
        @DisplayName("should have same score in HTML and Markdown")
        void shouldHaveSameScore() {
            // When
            String html = htmlRenderer.render(testData, diagrams);
            String markdown = markdownRenderer.render(testData, diagrams);

            // Then - both should contain the same score
            assertThat(html).contains("73");
            assertThat(markdown).contains("73");
        }

        @Test
        @DisplayName("should have same grade in HTML and Markdown")
        void shouldHaveSameGrade() {
            // When
            String html = htmlRenderer.render(testData, diagrams);
            String markdown = markdownRenderer.render(testData, diagrams);

            // Then - both should contain the same grade
            assertThat(html).contains("Grade C");
            assertThat(markdown).contains("**C**");
        }

        @Test
        @DisplayName("should have same status in HTML and Markdown")
        void shouldHaveSameStatus() {
            // When
            String html = htmlRenderer.render(testData, diagrams);
            String markdown = markdownRenderer.render(testData, diagrams);

            // Then - both should contain the same status
            assertThat(html).contains("Failed");
            assertThat(markdown).contains("FAILED");
        }
    }

    @Nested
    @DisplayName("Section Parity")
    class SectionParity {

        @Test
        @DisplayName("should have all 5 sections in HTML")
        void htmlShouldHaveAllSections() {
            // When
            String html = htmlRenderer.render(testData, diagrams);

            // Then
            assertThat(html).contains("id=\"verdict\"");
            assertThat(html).contains("id=\"architecture\"");
            assertThat(html).contains("id=\"issues\"");
            assertThat(html).contains("id=\"remediation\"");
            assertThat(html).contains("id=\"appendix\"");
        }

        @Test
        @DisplayName("should have all 5 sections in Markdown")
        void markdownShouldHaveAllSections() {
            // When
            String markdown = markdownRenderer.render(testData, diagrams);

            // Then
            assertThat(markdown).contains("## 1. Verdict");
            assertThat(markdown).contains("## 2. Architecture");
            assertThat(markdown).contains("## 3. Issues");
            assertThat(markdown).contains("## 4. Remediation");
            assertThat(markdown).contains("## 5. Appendix");
        }
    }

    @Nested
    @DisplayName("Diagram Encoding Parity")
    class DiagramEncodingParity {

        @Test
        @DisplayName("HTML should encode stereotype brackets as HTML entities")
        void htmlShouldEncodeStereotypeBrackets() {
            // Given - diagram with stereotypes
            DiagramSet diagramsWithStereotypes = createDiagramsWithStereotypes();

            // When
            String html = htmlRenderer.render(testData, diagramsWithStereotypes);

            // Then - HTML should have encoded entities
            assertThat(html).contains("&lt;&lt;AggregateRoot&gt;&gt;");
            assertThat(html).doesNotContain("<<AggregateRoot>>");
        }

        @Test
        @DisplayName("Markdown should keep stereotype brackets as direct characters")
        void markdownShouldKeepStereotypeBracketsDirect() {
            // Given - diagram with stereotypes
            DiagramSet diagramsWithStereotypes = createDiagramsWithStereotypes();

            // When
            String markdown = markdownRenderer.render(testData, diagramsWithStereotypes);

            // Then - Markdown should have direct characters
            assertThat(markdown).contains("<<AggregateRoot>>");
            assertThat(markdown).doesNotContain("&lt;&lt;AggregateRoot&gt;&gt;");
        }

        @Test
        @DisplayName("both renderers should have same diagram content (ignoring encoding)")
        void bothShouldHaveSameDiagramContent() {
            // Given - diagram with stereotypes
            DiagramSet diagramsWithStereotypes = createDiagramsWithStereotypes();

            // When
            String html = htmlRenderer.render(testData, diagramsWithStereotypes);
            String markdown = markdownRenderer.render(testData, diagramsWithStereotypes);

            // Then - both should contain the class name
            assertThat(html).contains("class Order");
            assertThat(markdown).contains("class Order");

            // And both should reference the diagram title
            assertThat(html).contains("Domain Model");
            assertThat(markdown).contains("Domain Model");
        }
    }

    @Nested
    @DisplayName("Violation Count Parity")
    class ViolationCountParity {

        @Test
        @DisplayName("should have same violation count in HTML and Markdown")
        void shouldHaveSameViolationCount() {
            // Given - data with violations
            ReportData dataWithViolations = createDataWithViolations();

            // When
            String html = htmlRenderer.render(dataWithViolations, diagrams);
            String markdown = markdownRenderer.render(dataWithViolations, diagrams);

            // Then - both should contain the same total
            assertThat(html).contains("3 violations");
            assertThat(markdown).contains("Total Issues:** 3");
        }
    }

    // Helper methods

    private ReportData createTestReportData() {
        var metadata = new ReportMetadata("Test Project", "1.0", Instant.now(), "10ms", "5.0.0", "5.0.0");

        var verdict = new Verdict(
                73, "C", ReportStatus.FAILED, "Score below threshold", "Summary", List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 1, 1);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = ComponentDetails.of(
                List.of(AggregateComponent.of("Order", "pkg", 5, List.of(), List.of())),
                List.of(ValueObjectComponent.of("Money", "pkg")),
                List.of(new IdentifierComponent("OrderId", "pkg", "UUID")),
                List.of(PortComponent.driving("OrderUseCase", "pkg", 3, false, null, List.of("Order"))),
                List.of(PortComponent.driven("OrderRepo", "pkg", "REPOSITORY", 4, false, null)),
                List.of());
        var architecture =
                new ArchitectureOverview("Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

        var issues = IssuesSummary.empty();
        var remediation = RemediationPlan.empty();
        var appendix = new Appendix(
                createScoreBreakdown(78, 60, 100, 24, 100),
                List.of(),
                List.of(),
                List.of(new PackageMetric("pkg", 1, 1, 0.5, 0.0, 0.5, PackageMetric.ZoneType.STABLE_CORE)));

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }

    private ReportData createDataWithViolations() {
        var metadata = new ReportMetadata("Test Project", "1.0", Instant.now(), "10ms", "5.0.0", "5.0.0");

        var verdict = new Verdict(
                60, "D", ReportStatus.FAILED, "Violations found", "Summary", List.of(), ImmediateAction.none());

        var totals = new InventoryTotals(1, 0, 2, 1, 0, 1, 1);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Test", 1, 0, 2, 0)), totals);
        var components = ComponentDetails.of(
                List.of(AggregateComponent.of("Order", "pkg", 5, List.of(), List.of())),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        var architecture =
                new ArchitectureOverview("Test arch", inventory, components, DiagramsInfo.defaults(), List.of());

        // Create violations
        var issueEntry1 = new IssueEntry(
                "V001",
                "ddd:test",
                Severity.MAJOR,
                "Test Issue 1",
                "Message 1",
                SourceLocation.at("Order", "Order.java", 10),
                "Impact 1",
                Suggestion.complete("Fix it", List.of("Step 1"), null, null));
        var issueEntry2 = new IssueEntry(
                "V002",
                "ddd:test2",
                Severity.MINOR,
                "Test Issue 2",
                "Message 2",
                SourceLocation.at("Money", "Money.java", 20),
                "Impact 2",
                Suggestion.complete("Fix it", List.of("Step 1"), null, null));
        var issueEntry3 = new IssueEntry(
                "V003",
                "ddd:test3",
                Severity.CRITICAL,
                "Test Issue 3",
                "Message 3",
                SourceLocation.at("OrderId", "OrderId.java", 5),
                "Impact 3",
                Suggestion.complete("Fix it", List.of("Step 1"), null, null));

        var issueGroup = new IssueGroup(
                "G1", "Test Group", "icon", "Description", 3, List.of(issueEntry1, issueEntry2, issueEntry3));
        var counts = new ViolationCounts(3, 0, 1, 1, 1, 0);
        var issues = new IssuesSummary(counts, List.of(issueGroup));

        var remediation = RemediationPlan.empty();
        var appendix = new Appendix(createScoreBreakdown(60, 50, 80, 40, 70), List.of(), List.of(), List.of());

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }

    private DiagramSet createTestDiagramSet() {
        return DiagramSet.builder()
                .scoreRadar("radar-beta\n  title Test\n  axis a, b, c\n  curve score {50, 60, 70}")
                .c4Context("C4Context\n  title Test")
                .c4Component("C4Component\n  title Test")
                .domainModel("classDiagram\n  class Order")
                .violationsPie("pie\n  \"MAJOR\" : 1")
                .packageZones("quadrantChart\n  title Test")
                .build();
    }

    private DiagramSet createDiagramsWithStereotypes() {
        String domainModel = """
            classDiagram
                class Order{
                    <<AggregateRoot>>
                    +fields 5
                }
                class Money{
                    <<ValueObject>>
                }
            """;

        return DiagramSet.builder()
                .scoreRadar("radar-beta\n  title Test\n  axis a, b, c\n  curve score {50, 60, 70}")
                .c4Context("C4Context\n  title Test")
                .c4Component("C4Component\n  title Test")
                .domainModel(domainModel)
                .violationsPie("pie\n  \"MAJOR\" : 1")
                .packageZones("quadrantChart\n  title Test")
                .build();
    }

    private ScoreBreakdown createScoreBreakdown(int ddd, int hex, int dep, int cpl, int coh) {
        return new ScoreBreakdown(
                ScoreDimension.of(25, ddd),
                ScoreDimension.of(25, hex),
                ScoreDimension.of(20, dep),
                ScoreDimension.of(15, cpl),
                ScoreDimension.of(15, coh));
    }
}
