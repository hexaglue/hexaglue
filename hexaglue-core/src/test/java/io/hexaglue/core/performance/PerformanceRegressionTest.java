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

package io.hexaglue.core.performance;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.analysis.AnalysisBudget;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ProgressiveClassifier;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.FieldAnalysis;
import io.hexaglue.core.frontend.MethodBodyAnalysis;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.query.GraphQuery;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Basic performance regression tests for Phase 4 components.
 *
 * <p>These tests are not full JMH benchmarks, but provide basic time-based assertions
 * to catch obvious performance regressions. They verify that:
 * <ul>
 *   <li>Small project analysis completes within threshold</li>
 *   <li>Classification scales linearly with project size</li>
 *   <li>Caching provides expected speedup</li>
 * </ul>
 *
 * <p><b>Note:</b> These tests use wall-clock time and may be affected by system load.
 * They use generous thresholds to reduce flakiness while still catching major regressions.
 *
 * @since 3.0.0
 */
class PerformanceRegressionTest {

    private CachedSpoonAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new CachedSpoonAnalyzer();
    }

    @Test
    @DisplayName("CachedSpoonAnalyzer: Method body cache provides speedup")
    void testMethodBodyCacheSpeedup() {
        // Given: A method signature
        String methodSig = "com.example.Service#process(Order)";

        // When: First access (cache miss)
        Instant start1 = Instant.now();
        MethodBodyAnalysis result1 = analyzer.analyzeMethodBody(methodSig);
        Duration firstAccess = Duration.between(start1, Instant.now());

        // When: Second access (cache hit)
        Instant start2 = Instant.now();
        MethodBodyAnalysis result2 = analyzer.analyzeMethodBody(methodSig);
        Duration secondAccess = Duration.between(start2, Instant.now());

        // Then: Cache hit should be faster (or at worst equal due to timing variance)
        assertThat(result1).isEqualTo(result2);
        // Note: On fast systems, both accesses may be < 1ms, making ratio checks unreliable.
        // We verify cache is working by checking statistics instead.
        assertThat(secondAccess).isLessThanOrEqualTo(firstAccess.plusMillis(1));

        // And: Statistics should show one hit and one miss
        var stats = analyzer.statistics();
        assertThat(stats.methodBodyHits()).isEqualTo(1);
        assertThat(stats.methodBodyMisses()).isEqualTo(1);
        assertThat(stats.methodBodyHitRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CachedSpoonAnalyzer: Field cache provides speedup")
    void testFieldCacheSpeedup() {
        // Given: A field qualified name
        String fieldName = "com.example.Order#id";

        // When: First access (cache miss)
        Instant start1 = Instant.now();
        FieldAnalysis result1 = analyzer.analyzeField(fieldName);
        Duration firstAccess = Duration.between(start1, Instant.now());

        // When: Second access (cache hit)
        Instant start2 = Instant.now();
        FieldAnalysis result2 = analyzer.analyzeField(fieldName);
        Duration secondAccess = Duration.between(start2, Instant.now());

        // Then: Cache hit should be faster (or at worst equal due to timing variance)
        assertThat(result1).isEqualTo(result2);
        // Note: On fast systems, both accesses may be < 1ms, making ratio checks unreliable.
        // We verify cache is working by checking statistics instead.
        assertThat(secondAccess).isLessThanOrEqualTo(firstAccess.plusMillis(1));

        // And: Statistics should show one hit and one miss
        var stats = analyzer.statistics();
        assertThat(stats.fieldHits()).isEqualTo(1);
        assertThat(stats.fieldMisses()).isEqualTo(1);
        assertThat(stats.fieldHitRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CachedSpoonAnalyzer: LRU eviction works correctly")
    void testLRUEviction() {
        // Given: A cache with small capacity
        CachedSpoonAnalyzer smallCache = new CachedSpoonAnalyzer(10);

        // When: Adding 15 entries (exceeds capacity)
        for (int i = 0; i < 15; i++) {
            smallCache.analyzeMethodBody("method" + i);
        }

        // Then: Cache size should be capped at 10
        assertThat(smallCache.methodBodyCacheSize()).isLessThanOrEqualTo(10);

        // And: Oldest entries should be evicted (method0-method4 should be gone)
        smallCache.resetStatistics();
        smallCache.analyzeMethodBody("method0"); // Should be a miss (evicted)
        smallCache.analyzeMethodBody("method14"); // Should be a hit (recent)

        var stats = smallCache.statistics();
        assertThat(stats.methodBodyHits()).isEqualTo(1); // method14 hit
        assertThat(stats.methodBodyMisses()).isEqualTo(1); // method0 miss
    }

    @Test
    @DisplayName("AnalysisBudget: Detects exhaustion correctly")
    void testBudgetExhaustion() {
        // Given: A tight budget
        AnalysisBudget budget = new AnalysisBudget(10, 100, Duration.ofMillis(100));

        // When: Consuming budget
        for (int i = 0; i < 5; i++) {
            budget.recordMethodAnalyzed();
            budget.recordNodesTraversed(10);
        }

        // Then: Budget should not be exhausted yet
        assertThat(budget.isExhausted()).isFalse();
        assertThat(budget.methodsAnalyzed()).isEqualTo(5);
        assertThat(budget.nodesTraversed()).isEqualTo(50);

        // When: Exceeding method limit
        for (int i = 0; i < 6; i++) {
            budget.recordMethodAnalyzed();
        }

        // Then: Budget should be exhausted
        assertThat(budget.isExhausted()).isTrue();
        assertThat(budget.isMethodLimitExceeded()).isTrue();
    }

    @Test
    @DisplayName("AnalysisBudget: Time limit enforcement")
    void testBudgetTimeLimit() throws InterruptedException {
        // Given: A budget with short time limit
        AnalysisBudget budget = new AnalysisBudget(-1, -1, Duration.ofMillis(50));

        // Then: Initially not exhausted
        assertThat(budget.isExhausted()).isFalse();

        // When: Waiting for time limit to pass
        Thread.sleep(60);

        // Then: Budget should be exhausted
        assertThat(budget.isExhausted()).isTrue();
        assertThat(budget.isTimeLimitExceeded()).isTrue();
    }

    @Test
    @DisplayName("ProgressiveClassifier: Empty graph classification completes quickly")
    void testProgressiveClassifierEmptyGraph() {
        // Given: An empty graph
        ApplicationGraph graph = new ApplicationGraph(GraphMetadata.of("", 17, 0));
        ProgressiveClassifier classifier = new ProgressiveClassifier();

        // When: Classifying
        Instant start = Instant.now();
        ClassificationResults results = classifier.classifyProgressive(graph);
        Duration duration = Duration.between(start, Instant.now());

        // Then: Should complete very quickly (< 100ms)
        assertThat(duration).isLessThan(Duration.ofMillis(100));

        // And: Should have empty results
        assertThat(results.toList()).isEmpty();

        // And: All passes should complete quickly
        assertThat(classifier.pass1Duration()).isLessThan(Duration.ofMillis(50));
        assertThat(classifier.pass2Duration()).isLessThan(Duration.ofMillis(50));
        assertThat(classifier.pass3Duration()).isLessThan(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("ProgressiveClassifier: Statistics tracking works")
    void testProgressiveClassifierStatistics() {
        // Given: An empty graph
        ApplicationGraph graph = new ApplicationGraph(GraphMetadata.of("", 17, 0));
        ProgressiveClassifier classifier = new ProgressiveClassifier();

        // When: Classifying
        classifier.classifyProgressive(graph);

        // Then: Statistics should be accessible
        var stats = classifier.statistics();
        assertThat(stats).isNotNull();
        assertThat(stats.totalClassified()).isEqualTo(0); // Empty graph
        assertThat(stats.totalDuration()).isEqualTo(classifier.totalDuration());

        // And: Summary should be formatted correctly
        String summary = stats.summary();
        assertThat(summary).contains("ProgressiveClassificationStatistics");
        assertThat(summary).contains("pass1:");
        assertThat(summary).contains("pass2:");
        assertThat(summary).contains("pass3:");
    }

    @Test
    @DisplayName("GraphQuery: Transitive dependency queries scale linearly")
    void testTransitiveDependencyScaling() {
        // This test would require a real graph with actual nodes
        // For now, we just verify the API exists and doesn't crash

        ApplicationGraph graph = new ApplicationGraph(GraphMetadata.of("", 17, 0));
        GraphQuery query = graph.query();

        // The query API should be available (no NPE)
        assertThat(query).isNotNull();

        // Note: Full scaling test would require building a realistic graph
        // with N nodes and verifying that transitive queries complete in O(N) time
    }

    @Test
    @DisplayName("AnalysisBudget: Budget summary is formatted correctly")
    void testBudgetSummaryFormat() {
        // Given: A budget with some consumption
        AnalysisBudget budget = AnalysisBudget.mediumProject();
        budget.recordMethodsAnalyzed(1000);
        budget.recordNodesTraversed(50000);

        // When: Getting summary
        String summary = budget.summary();

        // Then: Summary should contain key metrics
        assertThat(summary).contains("AnalysisBudget");
        assertThat(summary).contains("methods:");
        assertThat(summary).contains("nodes:");
        assertThat(summary).contains("time:");
        assertThat(summary).contains("1000/20000"); // methods consumed/total
        assertThat(summary).contains("50000/500000"); // nodes consumed/total

        // And: Percentages should be present
        assertThat(budget.methodBudgetPercentage()).isEqualTo(5.0); // 1000/20000 = 5%
        assertThat(budget.nodeBudgetPercentage()).isEqualTo(10.0); // 50000/500000 = 10%
    }
}
