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

package io.hexaglue.plugin.audit.domain.model.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the report data model classes.
 */
class ReportDataTest {

    @Nested
    @DisplayName("ReportData")
    class ReportDataTests {

        @Test
        @DisplayName("should create report with all sections")
        void shouldCreateReportWithAllSections() {
            // Given
            var metadata = createMetadata();
            var verdict = createVerdict(75, ReportStatus.PASSED_WITH_WARNINGS);
            var architecture = createArchitecture();
            var issues = IssuesSummary.empty();
            var remediation = RemediationPlan.empty();
            var appendix = createAppendix();

            // When
            var report = ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);

            // Then
            assertThat(report.version()).isEqualTo("2.0");
            assertThat(report.metadata()).isEqualTo(metadata);
            assertThat(report.verdict()).isEqualTo(verdict);
            assertThat(report.architecture()).isEqualTo(architecture);
            assertThat(report.issues()).isEqualTo(issues);
            assertThat(report.remediation()).isEqualTo(remediation);
            assertThat(report.appendix()).isEqualTo(appendix);
        }

        @Test
        @DisplayName("should report passed status correctly")
        void shouldReportPassedStatusCorrectly() {
            var report = createReportWithStatus(ReportStatus.PASSED);
            assertThat(report.passed()).isTrue();

            report = createReportWithStatus(ReportStatus.PASSED_WITH_WARNINGS);
            assertThat(report.passed()).isTrue();

            report = createReportWithStatus(ReportStatus.FAILED);
            assertThat(report.passed()).isFalse();
        }

        @Test
        @DisplayName("should reject null sections")
        void shouldRejectNullSections() {
            assertThatThrownBy(
                            () -> ReportData.create(
                                    null, createVerdict(75, ReportStatus.PASSED), createArchitecture(),
                                    IssuesSummary.empty(), RemediationPlan.empty(), createAppendix()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("metadata");
        }

        private ReportData createReportWithStatus(ReportStatus status) {
            return ReportData.create(
                    createMetadata(),
                    createVerdict(75, status),
                    createArchitecture(),
                    IssuesSummary.empty(),
                    RemediationPlan.empty(),
                    createAppendix());
        }
    }

    @Nested
    @DisplayName("Verdict")
    class VerdictTests {

        @Test
        @DisplayName("should compute grade correctly")
        void shouldComputeGradeCorrectly() {
            assertThat(Verdict.computeGrade(95)).isEqualTo("A");
            assertThat(Verdict.computeGrade(90)).isEqualTo("A");
            assertThat(Verdict.computeGrade(85)).isEqualTo("B");
            assertThat(Verdict.computeGrade(75)).isEqualTo("C");
            assertThat(Verdict.computeGrade(65)).isEqualTo("D");
            assertThat(Verdict.computeGrade(50)).isEqualTo("F");
        }

        @Test
        @DisplayName("should reject invalid score")
        void shouldRejectInvalidScore() {
            assertThatThrownBy(() -> new Verdict(-1, "F", ReportStatus.FAILED, null, "Test", List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("score must be between 0 and 100");

            assertThatThrownBy(() -> new Verdict(101, "A", ReportStatus.PASSED, null, "Test", List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should return immediate action if required")
        void shouldReturnImmediateActionIfRequired() {
            var action = ImmediateAction.required("Fix the cycle", "#issue-1");
            var verdict = new Verdict(50, "F", ReportStatus.FAILED, "Blocker found", "Summary", List.of(), action);

            assertThat(verdict.immediateActionOpt()).isPresent().contains(action);
        }

        @Test
        @DisplayName("should return empty immediate action if not required")
        void shouldReturnEmptyImmediateActionIfNotRequired() {
            var verdict = new Verdict(90, "A", ReportStatus.PASSED, null, "Summary", List.of(), ImmediateAction.none());

            assertThat(verdict.immediateActionOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("IssueEntry")
    class IssueEntryTests {

        @Test
        @DisplayName("should require impact and suggestion")
        void shouldRequireImpactAndSuggestion() {
            var location = SourceLocation.at("com.example.Order", "Order.java", 45);

            assertThatThrownBy(() -> new IssueEntry(
                            "id-1",
                            "ddd:constraint",
                            io.hexaglue.plugin.audit.domain.model.Severity.MAJOR,
                            "Title",
                            "Message",
                            location,
                            null, // impact is null
                            Suggestion.simple("Fix it")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("impact is mandatory");

            assertThatThrownBy(() -> new IssueEntry(
                            "id-1",
                            "ddd:constraint",
                            io.hexaglue.plugin.audit.domain.model.Severity.MAJOR,
                            "Title",
                            "Message",
                            location,
                            "Impact description",
                            null)) // suggestion is null
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("suggestion is mandatory");
        }

        @Test
        @DisplayName("should identify blocker issues")
        void shouldIdentifyBlockerIssues() {
            var entry = createIssueEntry(io.hexaglue.plugin.audit.domain.model.Severity.BLOCKER);
            assertThat(entry.isBlocker()).isTrue();
            assertThat(entry.isCriticalOrBlocker()).isTrue();

            entry = createIssueEntry(io.hexaglue.plugin.audit.domain.model.Severity.CRITICAL);
            assertThat(entry.isBlocker()).isFalse();
            assertThat(entry.isCriticalOrBlocker()).isTrue();

            entry = createIssueEntry(io.hexaglue.plugin.audit.domain.model.Severity.MAJOR);
            assertThat(entry.isBlocker()).isFalse();
            assertThat(entry.isCriticalOrBlocker()).isFalse();
        }

        private IssueEntry createIssueEntry(io.hexaglue.plugin.audit.domain.model.Severity severity) {
            return new IssueEntry(
                    "id-1",
                    "ddd:constraint",
                    severity,
                    "Title",
                    "Message",
                    SourceLocation.at("com.example.Order", "Order.java", 45),
                    "Impact description",
                    Suggestion.simple("Fix it"));
        }
    }

    @Nested
    @DisplayName("Suggestion")
    class SuggestionTests {

        @Test
        @DisplayName("should create simple suggestion")
        void shouldCreateSimpleSuggestion() {
            var suggestion = Suggestion.simple("Fix the issue");

            assertThat(suggestion.action()).isEqualTo("Fix the issue");
            assertThat(suggestion.steps()).isEmpty();
            assertThat(suggestion.hasSteps()).isFalse();
            assertThat(suggestion.hasCodeExample()).isFalse();
        }

        @Test
        @DisplayName("should create complete suggestion")
        void shouldCreateCompleteSuggestion() {
            var suggestion =
                    Suggestion.complete("Fix it", List.of("Step 1", "Step 2"), "public class Fixed {}", "2 days");

            assertThat(suggestion.action()).isEqualTo("Fix it");
            assertThat(suggestion.steps()).containsExactly("Step 1", "Step 2");
            assertThat(suggestion.hasSteps()).isTrue();
            assertThat(suggestion.codeExampleOpt()).contains("public class Fixed {}");
            assertThat(suggestion.hasCodeExample()).isTrue();
            assertThat(suggestion.effortOpt()).contains("2 days");
        }

        @Test
        @DisplayName("should defensively copy steps list")
        void shouldDefensivelyCopyStepsList() {
            var mutableSteps = new java.util.ArrayList<>(List.of("Step 1"));
            var suggestion = new Suggestion("Action", mutableSteps, null, null);

            mutableSteps.add("Step 2");

            assertThat(suggestion.steps()).containsExactly("Step 1");
        }
    }

    @Nested
    @DisplayName("ViolationCounts")
    class ViolationCountsTests {

        @Test
        @DisplayName("should count violations correctly")
        void shouldCountViolationsCorrectly() {
            var counts = new ViolationCounts(11, 1, 1, 9, 0, 0);

            assertThat(counts.total()).isEqualTo(11);
            assertThat(counts.blockers()).isEqualTo(1);
            assertThat(counts.criticals()).isEqualTo(1);
            assertThat(counts.majors()).isEqualTo(9);
            assertThat(counts.hasBlockers()).isTrue();
            assertThat(counts.hasCriticalOrBlocker()).isTrue();
        }

        @Test
        @DisplayName("should detect no blockers")
        void shouldDetectNoBlockers() {
            var counts = new ViolationCounts(5, 0, 0, 5, 0, 0);

            assertThat(counts.hasBlockers()).isFalse();
            assertThat(counts.hasCriticalOrBlocker()).isFalse();
        }
    }

    @Nested
    @DisplayName("ScoreBreakdown")
    class ScoreBreakdownTests {

        @Test
        @DisplayName("should calculate total score")
        void shouldCalculateTotalScore() {
            var breakdown = new ScoreBreakdown(
                    ScoreDimension.of(25, 78),
                    ScoreDimension.of(25, 60),
                    ScoreDimension.of(20, 100),
                    ScoreDimension.of(15, 24),
                    ScoreDimension.of(15, 100));

            // Expected: 25*78/100 + 25*60/100 + 20*100/100 + 15*24/100 + 15*100/100
            // = 19.5 + 15 + 20 + 3.6 + 15 = 73.1
            assertThat(breakdown.totalScore()).isCloseTo(73.1, org.assertj.core.api.Assertions.within(0.01));
            assertThat(breakdown.totalScoreRounded()).isEqualTo(73);
            assertThat(breakdown.hasValidWeights()).isTrue();
        }
    }

    @Nested
    @DisplayName("DiagramSet")
    class DiagramSetTests {

        @Test
        @DisplayName("should require all diagrams")
        void shouldRequireAllDiagrams() {
            assertThatThrownBy(() -> DiagramSet.builder().build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("scoreRadar");
        }

        @Test
        @DisplayName("should wrap diagram in code block")
        void shouldWrapDiagramInCodeBlock() {
            var diagram = "pie showData title Test\n    \"A\" : 1\n    \"B\" : 2";
            var wrapped = DiagramSet.wrapInCodeBlock(diagram);

            assertThat(wrapped).startsWith("```mermaid\n").endsWith("\n```").contains(diagram);
        }
    }

    // Helper methods

    private ReportMetadata createMetadata() {
        return new ReportMetadata(
                "Test Project", "1.0.0", Instant.now(), "42ms", "2.0.0-SNAPSHOT", "2.0.0-SNAPSHOT");
    }

    private Verdict createVerdict(int score, ReportStatus status) {
        return new Verdict(
                score,
                Verdict.computeGrade(score),
                status,
                null,
                "Test summary",
                List.of(KPI.percentage("test", "Test KPI", 80, 25, 90)),
                ImmediateAction.none());
    }

    private ArchitectureOverview createArchitecture() {
        var totals = new InventoryTotals(3, 0, 7, 4, 0, 1, 5);
        var inventory = new Inventory(List.of(new BoundedContextInventory("Default", 3, 0, 7, 0)), totals);
        return new ArchitectureOverview(
                "Test architecture", inventory, ComponentDetails.empty(), DiagramsInfo.defaults(), List.of());
    }

    private Appendix createAppendix() {
        var breakdown = new ScoreBreakdown(
                ScoreDimension.of(25, 78),
                ScoreDimension.of(25, 60),
                ScoreDimension.of(20, 100),
                ScoreDimension.of(15, 24),
                ScoreDimension.of(15, 100));
        return new Appendix(breakdown, List.of(), List.of(), List.of());
    }
}
