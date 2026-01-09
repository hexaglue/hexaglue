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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Budget enforcement for analysis operations to prevent runaway analysis.
 *
 * <p>This class tracks resource consumption during analysis and enforces limits
 * on methods analyzed, nodes traversed, and total time spent. It provides thread-safe
 * counters that can be checked during expensive operations to decide whether to
 * continue or abort early.
 *
 * <p>Budget enforcement is critical for:
 * <ul>
 *   <li>Preventing timeouts on large codebases (10,000+ types)</li>
 *   <li>Ensuring predictable build times in CI/CD pipelines</li>
 *   <li>Detecting pathological cases (cyclic graphs, infinite recursion)</li>
 *   <li>Trading completeness for performance when needed</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * AnalysisBudget budget = AnalysisBudget.smallProject();
 *
 * for (CtMethod<?> method : methods) {
 *     if (budget.isExhausted()) {
 *         log.warn("Analysis budget exhausted, skipping remaining methods");
 *         break;
 *     }
 *
 *     analyzeMethod(method);
 *     budget.recordMethodAnalyzed();
 * }
 *
 * log.info("Analysis complete: {}", budget.summary());
 * }</pre>
 *
 * @since 3.0.0
 */
public final class AnalysisBudget {

    private final int maxMethodsAnalyzed;
    private final long maxNodesTraversed;
    private final Duration maxTime;
    private final Instant startTime;

    private final AtomicInteger methodsAnalyzed = new AtomicInteger(0);
    private final AtomicLong nodesTraversed = new AtomicLong(0);

    /**
     * Creates an analysis budget with the given limits.
     *
     * @param maxMethodsAnalyzed maximum number of methods to analyze (-1 for unlimited)
     * @param maxNodesTraversed maximum number of AST nodes to traverse (-1 for unlimited)
     * @param maxTime maximum time duration (null for unlimited)
     * @throws IllegalArgumentException if any limit is invalid (negative but not -1)
     */
    public AnalysisBudget(int maxMethodsAnalyzed, long maxNodesTraversed, Duration maxTime) {
        if (maxMethodsAnalyzed < -1) {
            throw new IllegalArgumentException("maxMethodsAnalyzed must be >= -1 (got " + maxMethodsAnalyzed + ")");
        }
        if (maxNodesTraversed < -1) {
            throw new IllegalArgumentException("maxNodesTraversed must be >= -1 (got " + maxNodesTraversed + ")");
        }
        if (maxTime != null && maxTime.isNegative()) {
            throw new IllegalArgumentException("maxTime must be non-negative (got " + maxTime + ")");
        }

        this.maxMethodsAnalyzed = maxMethodsAnalyzed;
        this.maxNodesTraversed = maxNodesTraversed;
        this.maxTime = maxTime;
        this.startTime = Instant.now();
    }

    /**
     * Returns a budget suitable for small projects (100-500 types).
     *
     * <p>Small project limits:
     * <ul>
     *   <li>Methods analyzed: 5,000</li>
     *   <li>Nodes traversed: 100,000</li>
     *   <li>Time: 30 seconds</li>
     * </ul>
     *
     * @return a small project budget
     */
    public static AnalysisBudget smallProject() {
        return new AnalysisBudget(5_000, 100_000, Duration.ofSeconds(30));
    }

    /**
     * Returns a budget suitable for medium projects (500-2000 types).
     *
     * <p>Medium project limits:
     * <ul>
     *   <li>Methods analyzed: 20,000</li>
     *   <li>Nodes traversed: 500,000</li>
     *   <li>Time: 60 seconds</li>
     * </ul>
     *
     * @return a medium project budget
     */
    public static AnalysisBudget mediumProject() {
        return new AnalysisBudget(20_000, 500_000, Duration.ofSeconds(60));
    }

    /**
     * Returns a budget suitable for large projects (2000+ types).
     *
     * <p>Large project limits:
     * <ul>
     *   <li>Methods analyzed: 50,000</li>
     *   <li>Nodes traversed: 2,000,000</li>
     *   <li>Time: 180 seconds (3 minutes)</li>
     * </ul>
     *
     * @return a large project budget
     */
    public static AnalysisBudget largeProject() {
        return new AnalysisBudget(50_000, 2_000_000, Duration.ofMinutes(3));
    }

    /**
     * Returns an unlimited budget (no limits).
     *
     * <p>This budget never reports exhaustion and should only be used for:
     * <ul>
     *   <li>Testing and debugging</li>
     *   <li>One-off analysis where time is not constrained</li>
     *   <li>Situations where completeness is critical</li>
     * </ul>
     *
     * @return an unlimited budget
     */
    public static AnalysisBudget unlimited() {
        return new AnalysisBudget(-1, -1, null);
    }

    /**
     * Records that one method has been analyzed.
     *
     * <p>Thread-safe: can be called concurrently from multiple threads.
     */
    public void recordMethodAnalyzed() {
        methodsAnalyzed.incrementAndGet();
    }

    /**
     * Records that N methods have been analyzed.
     *
     * <p>Thread-safe: can be called concurrently from multiple threads.
     *
     * @param count the number of methods analyzed
     * @throws IllegalArgumentException if count is negative
     */
    public void recordMethodsAnalyzed(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0 (got " + count + ")");
        }
        methodsAnalyzed.addAndGet(count);
    }

    /**
     * Records that one AST node has been traversed.
     *
     * <p>Thread-safe: can be called concurrently from multiple threads.
     */
    public void recordNodeTraversed() {
        nodesTraversed.incrementAndGet();
    }

    /**
     * Records that N AST nodes have been traversed.
     *
     * <p>Thread-safe: can be called concurrently from multiple threads.
     *
     * @param count the number of nodes traversed
     * @throws IllegalArgumentException if count is negative
     */
    public void recordNodesTraversed(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0 (got " + count + ")");
        }
        nodesTraversed.addAndGet(count);
    }

    /**
     * Returns true if any budget limit has been exceeded.
     *
     * <p>This method should be called periodically during expensive analysis to decide
     * whether to continue or abort early. It checks all three dimensions:
     * <ul>
     *   <li>Methods analyzed vs maxMethodsAnalyzed</li>
     *   <li>Nodes traversed vs maxNodesTraversed</li>
     *   <li>Elapsed time vs maxTime</li>
     * </ul>
     *
     * @return true if any limit is exceeded, false otherwise
     */
    public boolean isExhausted() {
        return isMethodLimitExceeded() || isNodeLimitExceeded() || isTimeLimitExceeded();
    }

    /**
     * Returns true if the method analysis limit has been exceeded.
     *
     * @return true if methods analyzed >= maxMethodsAnalyzed (when limit is set)
     */
    public boolean isMethodLimitExceeded() {
        return maxMethodsAnalyzed != -1 && methodsAnalyzed.get() >= maxMethodsAnalyzed;
    }

    /**
     * Returns true if the node traversal limit has been exceeded.
     *
     * @return true if nodes traversed >= maxNodesTraversed (when limit is set)
     */
    public boolean isNodeLimitExceeded() {
        return maxNodesTraversed != -1 && nodesTraversed.get() >= maxNodesTraversed;
    }

    /**
     * Returns true if the time limit has been exceeded.
     *
     * @return true if elapsed time >= maxTime (when limit is set)
     */
    public boolean isTimeLimitExceeded() {
        if (maxTime == null) {
            return false;
        }
        Duration elapsed = Duration.between(startTime, Instant.now());
        return elapsed.compareTo(maxTime) >= 0;
    }

    /**
     * Returns the number of methods analyzed so far.
     *
     * @return the method count
     */
    public int methodsAnalyzed() {
        return methodsAnalyzed.get();
    }

    /**
     * Returns the number of AST nodes traversed so far.
     *
     * @return the node count
     */
    public long nodesTraversed() {
        return nodesTraversed.get();
    }

    /**
     * Returns the elapsed time since budget creation.
     *
     * @return the elapsed duration
     */
    public Duration elapsedTime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Returns the percentage of methods budget consumed (0-100).
     *
     * @return the percentage used, or -1 if unlimited
     */
    public double methodBudgetPercentage() {
        if (maxMethodsAnalyzed == -1) {
            return -1.0;
        }
        return (methodsAnalyzed.get() * 100.0) / maxMethodsAnalyzed;
    }

    /**
     * Returns the percentage of nodes budget consumed (0-100).
     *
     * @return the percentage used, or -1 if unlimited
     */
    public double nodeBudgetPercentage() {
        if (maxNodesTraversed == -1) {
            return -1.0;
        }
        return (nodesTraversed.get() * 100.0) / maxNodesTraversed;
    }

    /**
     * Returns the percentage of time budget consumed (0-100).
     *
     * @return the percentage used, or -1 if unlimited
     */
    public double timeBudgetPercentage() {
        if (maxTime == null) {
            return -1.0;
        }
        Duration elapsed = elapsedTime();
        return (elapsed.toMillis() * 100.0) / maxTime.toMillis();
    }

    /**
     * Returns a human-readable summary of budget consumption.
     *
     * <p>Example output:
     * <pre>
     * AnalysisBudget[methods: 3247/5000 (64.9%), nodes: 87234/100000 (87.2%), time: 18.3s/30.0s (61.0%)]
     * </pre>
     *
     * @return the budget summary
     */
    public String summary() {
        StringBuilder sb = new StringBuilder("AnalysisBudget[");

        // Methods
        if (maxMethodsAnalyzed == -1) {
            sb.append("methods: ").append(methodsAnalyzed.get()).append(" (unlimited)");
        } else {
            sb.append(String.format(
                    "methods: %d/%d (%.1f%%)", methodsAnalyzed.get(), maxMethodsAnalyzed, methodBudgetPercentage()));
        }

        // Nodes
        if (maxNodesTraversed == -1) {
            sb.append(", nodes: ").append(nodesTraversed.get()).append(" (unlimited)");
        } else {
            sb.append(String.format(
                    ", nodes: %d/%d (%.1f%%)", nodesTraversed.get(), maxNodesTraversed, nodeBudgetPercentage()));
        }

        // Time
        Duration elapsed = elapsedTime();
        if (maxTime == null) {
            sb.append(String.format(", time: %.1fs (unlimited)", elapsed.toMillis() / 1000.0));
        } else {
            sb.append(String.format(
                    ", time: %.1fs/%.1fs (%.1f%%)",
                    elapsed.toMillis() / 1000.0, maxTime.toMillis() / 1000.0, timeBudgetPercentage()));
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns the start time of the budget.
     *
     * @return the start instant
     */
    public Instant startTime() {
        return startTime;
    }

    /**
     * Returns the maximum methods allowed.
     *
     * @return the limit, or -1 if unlimited
     */
    public int maxMethodsAnalyzed() {
        return maxMethodsAnalyzed;
    }

    /**
     * Returns the maximum nodes allowed.
     *
     * @return the limit, or -1 if unlimited
     */
    public long maxNodesTraversed() {
        return maxNodesTraversed;
    }

    /**
     * Returns the maximum time allowed.
     *
     * @return the limit, or null if unlimited
     */
    public Duration maxTime() {
        return maxTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AnalysisBudget other)) return false;
        return maxMethodsAnalyzed == other.maxMethodsAnalyzed
                && maxNodesTraversed == other.maxNodesTraversed
                && Objects.equals(maxTime, other.maxTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxMethodsAnalyzed, maxNodesTraversed, maxTime);
    }

    @Override
    public String toString() {
        return summary();
    }
}
