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

import io.hexaglue.core.graph.model.MethodNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Prioritizes methods for deep analysis based on visibility and importance.
 *
 * <p>Deep analysis of method bodies is expensive (AST traversal, invocation resolution,
 * data flow analysis). This prioritizer ensures that:
 * <ul>
 *   <li><b>Priority 1 (Critical):</b> Public methods - always analyzed</li>
 *   <li><b>Priority 2 (Important):</b> Protected methods - analyzed if budget allows</li>
 *   <li><b>Priority 3 (Optional):</b> Package-private methods - rarely analyzed</li>
 *   <li><b>Priority 4 (Skip):</b> Private methods - typically skipped</li>
 * </ul>
 *
 * <p>Rationale:
 * <ul>
 *   <li>Public methods define the external API and classification signals</li>
 *   <li>Protected methods may influence subclass behavior</li>
 *   <li>Package-private and private methods are implementation details</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * List<MethodNode> methods = graph.methodsOf(type);
 * AnalysisBudget budget = AnalysisBudget.smallProject();
 *
 * List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods, budget);
 *
 * for (MethodNode method : prioritized) {
 *     if (budget.isExhausted()) {
 *         log.warn("Budget exhausted after analyzing {} methods", budget.methodsAnalyzed());
 *         break;
 *     }
 *     analyzeMethodBody(method);
 *     budget.recordMethodAnalyzed();
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public final class PublicApiPrioritizer {

    /**
     * Priority level for method analysis.
     */
    public enum Priority {
        /**
         * Critical priority - public methods (always analyzed).
         */
        CRITICAL(1),

        /**
         * Important priority - protected methods (analyzed if budget allows).
         */
        IMPORTANT(2),

        /**
         * Optional priority - package-private methods (rarely analyzed).
         */
        OPTIONAL(3),

        /**
         * Skip priority - private methods (typically skipped).
         */
        SKIP(4);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        /**
         * Returns the numeric priority level (lower is higher priority).
         *
         * @return the priority level
         */
        public int level() {
            return level;
        }
    }

    private PublicApiPrioritizer() {
        // Utility class - no instantiation
    }

    /**
     * Prioritizes methods for analysis based on visibility and budget.
     *
     * <p>This method sorts methods by priority and returns a subset that fits
     * within the budget. The budget is checked to ensure we don't exceed limits.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Classify each method by priority (CRITICAL, IMPORTANT, OPTIONAL, SKIP)</li>
     *   <li>Sort methods by priority level</li>
     *   <li>Include methods up to budget limit, respecting priority order</li>
     * </ol>
     *
     * @param methods the methods to prioritize
     * @param budget the analysis budget
     * @return the prioritized list of methods to analyze
     */
    public static List<MethodNode> prioritize(List<MethodNode> methods, AnalysisBudget budget) {
        if (methods.isEmpty()) {
            return List.of();
        }

        // Calculate budget for method body analysis
        int remainingBudget = calculateRemainingBudget(budget);

        // Sort by priority
        List<MethodNode> sorted = methods.stream()
                .sorted(Comparator.comparing(PublicApiPrioritizer::priorityOf))
                .toList();

        // Take methods up to budget
        List<MethodNode> result = new ArrayList<>();
        for (MethodNode method : sorted) {
            Priority priority = priorityOf(method);

            // Always skip private methods unless unlimited budget
            if (priority == Priority.SKIP && budget.maxMethodsAnalyzed() != -1) {
                continue;
            }

            // Check budget
            if (remainingBudget != -1 && result.size() >= remainingBudget) {
                break;
            }

            result.add(method);
        }

        return result;
    }

    /**
     * Prioritizes methods without budget constraints.
     *
     * <p>This variant includes all methods except private ones, sorted by priority.
     *
     * @param methods the methods to prioritize
     * @return the prioritized list of methods
     */
    public static List<MethodNode> prioritize(List<MethodNode> methods) {
        // Use a large budget to exclude private methods by default
        return prioritize(methods, AnalysisBudget.largeProject());
    }

    /**
     * Returns a stream of methods filtered and sorted by priority.
     *
     * <p>This variant provides a streaming API for more flexible consumption.
     *
     * @param methods the methods to prioritize
     * @return a stream of prioritized methods
     */
    public static Stream<MethodNode> prioritizeStream(List<MethodNode> methods) {
        return methods.stream()
                .filter(m -> priorityOf(m) != Priority.SKIP)
                .sorted(Comparator.comparing(PublicApiPrioritizer::priorityOf));
    }

    /**
     * Determines the priority of a method based on its visibility.
     *
     * <p>Priority rules:
     * <ul>
     *   <li>Public → CRITICAL</li>
     *   <li>Protected → IMPORTANT</li>
     *   <li>Package-private (no modifier) → OPTIONAL</li>
     *   <li>Private → SKIP</li>
     * </ul>
     *
     * @param method the method to prioritize
     * @return the priority level
     */
    public static Priority priorityOf(MethodNode method) {
        if (method.isPublic()) {
            return Priority.CRITICAL;
        } else if (method.isProtected()) {
            return Priority.IMPORTANT;
        } else if (method.isPrivate()) {
            return Priority.SKIP;
        } else {
            // Package-private (default visibility)
            return Priority.OPTIONAL;
        }
    }

    /**
     * Returns true if the method should be analyzed given the priority threshold.
     *
     * <p>This method checks if the method's priority is at or above the threshold.
     *
     * @param method the method to check
     * @param threshold the minimum priority level
     * @return true if the method should be analyzed
     */
    public static boolean shouldAnalyze(MethodNode method, Priority threshold) {
        return priorityOf(method).level() <= threshold.level();
    }

    /**
     * Filters methods to include only those at or above the priority threshold.
     *
     * @param methods the methods to filter
     * @param threshold the minimum priority level
     * @return the filtered list of methods
     */
    public static List<MethodNode> filterByPriority(List<MethodNode> methods, Priority threshold) {
        return methods.stream().filter(m -> shouldAnalyze(m, threshold)).toList();
    }

    /**
     * Returns the count of methods for each priority level.
     *
     * @param methods the methods to analyze
     * @return a record with counts by priority
     */
    public static PriorityCounts countByPriority(List<MethodNode> methods) {
        int critical = 0;
        int important = 0;
        int optional = 0;
        int skip = 0;

        for (MethodNode method : methods) {
            switch (priorityOf(method)) {
                case CRITICAL -> critical++;
                case IMPORTANT -> important++;
                case OPTIONAL -> optional++;
                case SKIP -> skip++;
            }
        }

        return new PriorityCounts(critical, important, optional, skip);
    }

    /**
     * Calculates the remaining budget for method body analysis.
     *
     * @param budget the analysis budget
     * @return the remaining number of methods to analyze, or -1 if unlimited
     */
    private static int calculateRemainingBudget(AnalysisBudget budget) {
        if (budget.maxMethodsAnalyzed() == -1) {
            return -1; // Unlimited
        }

        int remaining = budget.maxMethodsAnalyzed() - budget.methodsAnalyzed();
        return Math.max(0, remaining);
    }

    /**
     * Record containing method counts by priority level.
     *
     * @param critical number of critical (public) methods
     * @param important number of important (protected) methods
     * @param optional number of optional (package-private) methods
     * @param skip number of private methods (typically skipped)
     * @since 3.0.0
     */
    public record PriorityCounts(int critical, int important, int optional, int skip) {

        /**
         * Returns the total number of methods.
         *
         * @return the total count
         */
        public int total() {
            return critical + important + optional + skip;
        }

        /**
         * Returns the number of methods that should be analyzed (excluding SKIP).
         *
         * @return the count of analyzable methods
         */
        public int analyzable() {
            return critical + important + optional;
        }

        /**
         * Returns a human-readable summary.
         *
         * @return the summary string
         */
        public String summary() {
            return String.format(
                    "PriorityCounts[critical=%d, important=%d, optional=%d, skip=%d, total=%d]",
                    critical, important, optional, skip, total());
        }

        @Override
        public String toString() {
            return summary();
        }
    }
}
