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

package io.hexaglue.core.analysis;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AnalysisBudget}.
 */
class AnalysisBudgetTest {

    @Test
    void smallProject_shouldReturnExpectedLimits() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThat(budget.maxMethodsAnalyzed()).isEqualTo(5_000);
        assertThat(budget.maxNodesTraversed()).isEqualTo(100_000);
        assertThat(budget.maxTime()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void mediumProject_shouldReturnExpectedLimits() {
        AnalysisBudget budget = AnalysisBudget.mediumProject();

        assertThat(budget.maxMethodsAnalyzed()).isEqualTo(20_000);
        assertThat(budget.maxNodesTraversed()).isEqualTo(500_000);
        assertThat(budget.maxTime()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void largeProject_shouldReturnExpectedLimits() {
        AnalysisBudget budget = AnalysisBudget.largeProject();

        assertThat(budget.maxMethodsAnalyzed()).isEqualTo(50_000);
        assertThat(budget.maxNodesTraversed()).isEqualTo(2_000_000);
        assertThat(budget.maxTime()).isEqualTo(Duration.ofMinutes(3));
    }

    @Test
    void unlimited_shouldReturnUnlimitedBudget() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        assertThat(budget.maxMethodsAnalyzed()).isEqualTo(-1);
        assertThat(budget.maxNodesTraversed()).isEqualTo(-1);
        assertThat(budget.maxTime()).isNull();
    }

    @Test
    void constructor_shouldRejectInvalidLimits() {
        assertThatThrownBy(() -> new AnalysisBudget(-2, 1000, Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxMethodsAnalyzed");

        assertThatThrownBy(() -> new AnalysisBudget(1000, -5, Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxNodesTraversed");

        assertThatThrownBy(() -> new AnalysisBudget(1000, 1000, Duration.ofSeconds(-10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTime");
    }

    @Test
    void recordMethodAnalyzed_shouldIncrementCounter() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThat(budget.methodsAnalyzed()).isZero();

        budget.recordMethodAnalyzed();
        assertThat(budget.methodsAnalyzed()).isEqualTo(1);

        budget.recordMethodAnalyzed();
        budget.recordMethodAnalyzed();
        assertThat(budget.methodsAnalyzed()).isEqualTo(3);
    }

    @Test
    void recordMethodsAnalyzed_shouldIncrementByCount() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        budget.recordMethodsAnalyzed(10);
        assertThat(budget.methodsAnalyzed()).isEqualTo(10);

        budget.recordMethodsAnalyzed(5);
        assertThat(budget.methodsAnalyzed()).isEqualTo(15);
    }

    @Test
    void recordMethodsAnalyzed_shouldRejectNegativeCount() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThatThrownBy(() -> budget.recordMethodsAnalyzed(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count must be >= 0");
    }

    @Test
    void recordNodeTraversed_shouldIncrementCounter() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThat(budget.nodesTraversed()).isZero();

        budget.recordNodeTraversed();
        assertThat(budget.nodesTraversed()).isEqualTo(1);

        budget.recordNodeTraversed();
        budget.recordNodeTraversed();
        assertThat(budget.nodesTraversed()).isEqualTo(3);
    }

    @Test
    void recordNodesTraversed_shouldIncrementByCount() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        budget.recordNodesTraversed(100);
        assertThat(budget.nodesTraversed()).isEqualTo(100);

        budget.recordNodesTraversed(50);
        assertThat(budget.nodesTraversed()).isEqualTo(150);
    }

    @Test
    void recordNodesTraversed_shouldRejectNegativeCount() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThatThrownBy(() -> budget.recordNodesTraversed(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count must be >= 0");
    }

    @Test
    void isMethodLimitExceeded_shouldReturnTrueWhenLimitReached() {
        AnalysisBudget budget = new AnalysisBudget(10, 1000, Duration.ofSeconds(30));

        assertThat(budget.isMethodLimitExceeded()).isFalse();

        budget.recordMethodsAnalyzed(9);
        assertThat(budget.isMethodLimitExceeded()).isFalse();

        budget.recordMethodAnalyzed();
        assertThat(budget.isMethodLimitExceeded()).isTrue();

        budget.recordMethodAnalyzed();
        assertThat(budget.isMethodLimitExceeded()).isTrue();
    }

    @Test
    void isMethodLimitExceeded_shouldReturnFalseWhenUnlimited() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordMethodsAnalyzed(1_000_000);
        assertThat(budget.isMethodLimitExceeded()).isFalse();
    }

    @Test
    void isNodeLimitExceeded_shouldReturnTrueWhenLimitReached() {
        AnalysisBudget budget = new AnalysisBudget(1000, 100, Duration.ofSeconds(30));

        assertThat(budget.isNodeLimitExceeded()).isFalse();

        budget.recordNodesTraversed(99);
        assertThat(budget.isNodeLimitExceeded()).isFalse();

        budget.recordNodeTraversed();
        assertThat(budget.isNodeLimitExceeded()).isTrue();

        budget.recordNodeTraversed();
        assertThat(budget.isNodeLimitExceeded()).isTrue();
    }

    @Test
    void isNodeLimitExceeded_shouldReturnFalseWhenUnlimited() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordNodesTraversed(10_000_000);
        assertThat(budget.isNodeLimitExceeded()).isFalse();
    }

    @Test
    void isTimeLimitExceeded_shouldReturnTrueAfterTimeout() throws InterruptedException {
        AnalysisBudget budget = new AnalysisBudget(1000, 1000, Duration.ofMillis(50));

        assertThat(budget.isTimeLimitExceeded()).isFalse();

        Thread.sleep(100);

        assertThat(budget.isTimeLimitExceeded()).isTrue();
    }

    @Test
    void isTimeLimitExceeded_shouldReturnFalseWhenUnlimited() throws InterruptedException {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        Thread.sleep(50);

        assertThat(budget.isTimeLimitExceeded()).isFalse();
    }

    @Test
    void isExhausted_shouldReturnTrueIfAnyLimitExceeded() throws InterruptedException {
        // Test method limit
        AnalysisBudget budget1 = new AnalysisBudget(10, 1000, Duration.ofSeconds(30));
        budget1.recordMethodsAnalyzed(10);
        assertThat(budget1.isExhausted()).isTrue();

        // Test node limit
        AnalysisBudget budget2 = new AnalysisBudget(1000, 100, Duration.ofSeconds(30));
        budget2.recordNodesTraversed(100);
        assertThat(budget2.isExhausted()).isTrue();

        // Test time limit
        AnalysisBudget budget3 = new AnalysisBudget(1000, 1000, Duration.ofMillis(50));
        Thread.sleep(100);
        assertThat(budget3.isExhausted()).isTrue();
    }

    @Test
    void isExhausted_shouldReturnFalseWhenNoLimitExceeded() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        budget.recordMethodsAnalyzed(100);
        budget.recordNodesTraversed(1000);

        assertThat(budget.isExhausted()).isFalse();
    }

    @Test
    void isExhausted_shouldReturnFalseForUnlimitedBudget() throws InterruptedException {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordMethodsAnalyzed(1_000_000);
        budget.recordNodesTraversed(10_000_000);
        Thread.sleep(50);

        assertThat(budget.isExhausted()).isFalse();
    }

    @Test
    void methodBudgetPercentage_shouldCalculateCorrectly() {
        AnalysisBudget budget = new AnalysisBudget(100, 1000, Duration.ofSeconds(30));

        budget.recordMethodsAnalyzed(50);
        assertThat(budget.methodBudgetPercentage()).isEqualTo(50.0);

        budget.recordMethodsAnalyzed(25);
        assertThat(budget.methodBudgetPercentage()).isEqualTo(75.0);
    }

    @Test
    void methodBudgetPercentage_shouldReturnMinusOneForUnlimited() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordMethodsAnalyzed(1000);
        assertThat(budget.methodBudgetPercentage()).isEqualTo(-1.0);
    }

    @Test
    void nodeBudgetPercentage_shouldCalculateCorrectly() {
        AnalysisBudget budget = new AnalysisBudget(1000, 1000, Duration.ofSeconds(30));

        budget.recordNodesTraversed(250);
        assertThat(budget.nodeBudgetPercentage()).isEqualTo(25.0);

        budget.recordNodesTraversed(250);
        assertThat(budget.nodeBudgetPercentage()).isEqualTo(50.0);
    }

    @Test
    void nodeBudgetPercentage_shouldReturnMinusOneForUnlimited() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordNodesTraversed(10000);
        assertThat(budget.nodeBudgetPercentage()).isEqualTo(-1.0);
    }

    @Test
    void timeBudgetPercentage_shouldCalculateCorrectly() throws InterruptedException {
        AnalysisBudget budget = new AnalysisBudget(1000, 1000, Duration.ofMillis(100));

        Thread.sleep(50);

        double percentage = budget.timeBudgetPercentage();
        assertThat(percentage).isGreaterThanOrEqualTo(45.0).isLessThan(100.0);
    }

    @Test
    void timeBudgetPercentage_shouldReturnMinusOneForUnlimited() throws InterruptedException {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        Thread.sleep(50);

        assertThat(budget.timeBudgetPercentage()).isEqualTo(-1.0);
    }

    @Test
    void elapsedTime_shouldReturnNonZeroDuration() throws InterruptedException {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        Thread.sleep(50);

        Duration elapsed = budget.elapsedTime();
        assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(45);
    }

    @Test
    void summary_shouldIncludeAllMetrics() {
        AnalysisBudget budget = new AnalysisBudget(1000, 10000, Duration.ofSeconds(30));

        budget.recordMethodsAnalyzed(500);
        budget.recordNodesTraversed(5000);

        String summary = budget.summary();

        // Use locale-independent assertions (accept both "50.0%" and "50,0%")
        assertThat(summary)
                .contains("AnalysisBudget[")
                .contains("methods:")
                .contains("500/1000")
                .containsAnyOf("50.0%", "50,0%")
                .contains("nodes:")
                .contains("5000/10000")
                .contains("time:");
    }

    @Test
    void summary_shouldShowUnlimitedForUnboundedBudget() {
        AnalysisBudget budget = AnalysisBudget.unlimited();

        budget.recordMethodsAnalyzed(1000);
        budget.recordNodesTraversed(10000);

        String summary = budget.summary();

        assertThat(summary)
                .contains("methods: 1000 (unlimited)")
                .contains("nodes: 10000 (unlimited)")
                .contains("time:")
                .contains("(unlimited)");
    }

    @Test
    void equality_shouldWorkCorrectly() {
        AnalysisBudget budget1 = new AnalysisBudget(1000, 10000, Duration.ofSeconds(30));
        AnalysisBudget budget2 = new AnalysisBudget(1000, 10000, Duration.ofSeconds(30));
        AnalysisBudget budget3 = new AnalysisBudget(2000, 10000, Duration.ofSeconds(30));

        assertThat(budget1).isEqualTo(budget2);
        assertThat(budget1).isNotEqualTo(budget3);
        assertThat(budget1.hashCode()).isEqualTo(budget2.hashCode());
    }

    @Test
    void startTime_shouldBeSet() {
        AnalysisBudget budget = AnalysisBudget.smallProject();

        assertThat(budget.startTime()).isNotNull();
        assertThat(budget.startTime()).isBeforeOrEqualTo(java.time.Instant.now());
    }
}
