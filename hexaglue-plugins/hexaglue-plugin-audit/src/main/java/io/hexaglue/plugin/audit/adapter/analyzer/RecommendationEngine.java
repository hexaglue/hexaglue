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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import io.hexaglue.plugin.audit.domain.model.RecommendationPriority;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates prioritized recommendations from audit violations.
 *
 * <p>This analyzer examines violations from an {@link AuditResult} and produces
 * actionable recommendations prioritized by severity, architectural impact, and ROI.
 *
 * <p><strong>Recommendation grouping strategy:</strong>
 * <ul>
 *   <li>Groups violations by constraint ID (same architectural issue)</li>
 *   <li>Groups violations by package (same architectural zone)</li>
 *   <li>Creates architectural recommendations for cross-cutting concerns</li>
 * </ul>
 *
 * <p><strong>Priority assignment:</strong>
 * <ul>
 *   <li>{@link RecommendationPriority#IMMEDIATE IMMEDIATE}: BLOCKER/CRITICAL violations
 *       or architectural issues affecting system integrity</li>
 *   <li>{@link RecommendationPriority#SHORT_TERM SHORT_TERM}: MAJOR violations
 *       affecting multiple types (3+)</li>
 *   <li>{@link RecommendationPriority#MEDIUM_TERM MEDIUM_TERM}: MAJOR violations
 *       with isolated impact (1-2 types)</li>
 *   <li>{@link RecommendationPriority#LOW LOW}: MINOR/INFO violations</li>
 * </ul>
 *
 * <p><strong>Effort estimation:</strong>
 * Effort is calculated using the {@link DebtEstimator} default mapping:
 * BLOCKER=3.0d, CRITICAL=2.0d, MAJOR=0.5d, MINOR=0.25d, INFO=0.0d
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * RecommendationEngine engine = new RecommendationEngine(new DebtEstimator());
 * List<Recommendation> recommendations = engine.generateRecommendations(auditResult);
 *
 * recommendations.forEach(rec -> {
 *     System.out.println(rec.priority() + ": " + rec.title());
 *     System.out.println("  Effort: " + rec.estimatedEffort() + " days");
 *     System.out.println("  Impact: " + rec.expectedImpact());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public class RecommendationEngine {

    private static final int MULTIPLE_TYPES_THRESHOLD = 3;

    private final DebtEstimator debtEstimator;

    /**
     * Creates a recommendation engine with the default debt estimator.
     */
    public RecommendationEngine() {
        this(new DebtEstimator());
    }

    /**
     * Creates a recommendation engine with a custom debt estimator.
     *
     * @param debtEstimator the debt estimator for effort calculation
     * @throws NullPointerException if debtEstimator is null
     */
    public RecommendationEngine(DebtEstimator debtEstimator) {
        this.debtEstimator = Objects.requireNonNull(debtEstimator, "debtEstimator required");
    }

    /**
     * Generates prioritized recommendations from audit violations.
     *
     * <p>This method:
     * <ol>
     *   <li>Groups related violations by constraint ID</li>
     *   <li>Calculates priority based on severity and impact</li>
     *   <li>Estimates effort using the debt estimator</li>
     *   <li>Generates actionable descriptions and expected impacts</li>
     *   <li>Returns recommendations sorted by priority (highest first)</li>
     * </ol>
     *
     * <p>If the audit result contains no violations, returns an empty list.
     *
     * @param result the audit result containing violations
     * @return list of recommendations, sorted by priority (highest first)
     * @throws NullPointerException if result is null
     */
    public List<Recommendation> generateRecommendations(AuditResult result) {
        Objects.requireNonNull(result, "result required");

        if (result.violations().isEmpty()) {
            return List.of();
        }

        // Group violations by constraint ID
        Map<ConstraintId, List<Violation>> violationsByConstraint = result.violations().stream()
                .collect(Collectors.groupingBy(
                        Violation::constraintId, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        // Generate one recommendation per constraint group
        List<Recommendation> recommendations = violationsByConstraint.entrySet().stream()
                .map(entry -> createRecommendation(entry.getKey(), entry.getValue()))
                .toList();

        // Sort by priority (IMMEDIATE first, LOW last)
        return recommendations.stream()
                .sorted((r1, r2) -> r1.priority().compareTo(r2.priority()))
                .toList();
    }

    /**
     * Creates a single recommendation from a group of related violations.
     *
     * @param constraintId the constraint that was violated
     * @param violations   the violations of this constraint
     * @return the recommendation
     */
    private Recommendation createRecommendation(ConstraintId constraintId, List<Violation> violations) {
        // Determine priority based on violations
        RecommendationPriority priority = determinePriority(violations);

        // Extract affected types
        List<String> affectedTypes = violations.stream()
                .map(Violation::affectedTypes)
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        // Calculate effort
        double effort = debtEstimator.estimate(violations).totalDays();

        // Generate title and description
        String title = generateTitle(constraintId, violations);
        String description = generateDescription(constraintId, violations, affectedTypes);
        String expectedImpact = generateExpectedImpact(priority, affectedTypes.size());

        return Recommendation.builder()
                .priority(priority)
                .title(title)
                .description(description)
                .affectedTypes(affectedTypes)
                .estimatedEffort(effort)
                .expectedImpact(expectedImpact)
                .relatedViolations(violations.stream()
                        .map(v -> v.constraintId())
                        .distinct()
                        .toList())
                .build();
    }

    /**
     * Determines recommendation priority from violation characteristics.
     *
     * <p>Priority rules:
     * <ul>
     *   <li>IMMEDIATE: Any BLOCKER or CRITICAL violation</li>
     *   <li>IMMEDIATE: Architectural issues (aggregate boundaries, dependencies)</li>
     *   <li>SHORT_TERM: MAJOR violations affecting 3+ types</li>
     *   <li>MEDIUM_TERM: MAJOR violations affecting 1-2 types</li>
     *   <li>LOW: All MINOR and INFO violations</li>
     * </ul>
     */
    private RecommendationPriority determinePriority(List<Violation> violations) {
        // Check for blocker/critical violations
        boolean hasCriticalViolation = violations.stream()
                .anyMatch(v -> v.severity() == Severity.BLOCKER || v.severity() == Severity.CRITICAL);

        if (hasCriticalViolation) {
            return RecommendationPriority.IMMEDIATE;
        }

        // Check for architectural issues (by constraint category)
        boolean isArchitecturalIssue = violations.stream().anyMatch(v -> isArchitecturalConstraint(v.constraintId()));

        if (isArchitecturalIssue) {
            return RecommendationPriority.IMMEDIATE;
        }

        // Check for major violations
        boolean hasMajorViolation = violations.stream().anyMatch(v -> v.severity() == Severity.MAJOR);

        if (hasMajorViolation) {
            // Count affected types
            long affectedTypeCount = violations.stream()
                    .map(Violation::affectedTypes)
                    .flatMap(Collection::stream)
                    .distinct()
                    .count();

            return affectedTypeCount >= MULTIPLE_TYPES_THRESHOLD
                    ? RecommendationPriority.SHORT_TERM
                    : RecommendationPriority.MEDIUM_TERM;
        }

        // Default: minor/info violations
        return RecommendationPriority.LOW;
    }

    /**
     * Checks if a constraint is architectural in nature.
     *
     * <p>Architectural constraints include:
     * <ul>
     *   <li>ddd:aggregate-boundary (aggregate boundary violations)</li>
     *   <li>ddd:aggregate-deps (aggregate dependency issues)</li>
     *   <li>hexagonal:* (hexagonal architecture rules)</li>
     * </ul>
     */
    private boolean isArchitecturalConstraint(ConstraintId constraintId) {
        String category = constraintId.category();
        String name = constraintId.name();

        // Hexagonal architecture constraints are always architectural
        if ("hexagonal".equals(category)) {
            return true;
        }

        // DDD aggregate boundary and dependency constraints are architectural
        if ("ddd".equals(category) && ("aggregate-boundary".equals(name) || "aggregate-deps".equals(name))) {
            return true;
        }

        return false;
    }

    /**
     * Generates a concise title for the recommendation.
     */
    private String generateTitle(ConstraintId constraintId, List<Violation> violations) {
        String constraintName = formatConstraintName(constraintId);
        int violationCount = violations.size();

        if (violationCount == 1) {
            return constraintName;
        } else {
            return String.format("%s (%d violations)", constraintName, violationCount);
        }
    }

    /**
     * Generates a detailed description with actionable guidance.
     */
    private String generateDescription(
            ConstraintId constraintId, List<Violation> violations, List<String> affectedTypes) {
        StringBuilder desc = new StringBuilder();

        // Add violation summary
        desc.append("Constraint '").append(constraintId.value()).append("' has been violated.\n\n");

        // Add affected types summary
        if (!affectedTypes.isEmpty()) {
            desc.append("Affected types:\n");
            affectedTypes.stream()
                    .limit(5)
                    .forEach(type -> desc.append("  - ").append(type).append("\n"));
            if (affectedTypes.size() > 5) {
                desc.append("  ... and ").append(affectedTypes.size() - 5).append(" more\n");
            }
            desc.append("\n");
        }

        // Add specific violation messages (limit to first 3)
        desc.append("Issues:\n");
        violations.stream()
                .limit(3)
                .forEach(v -> desc.append("  - ").append(v.message()).append("\n"));

        if (violations.size() > 3) {
            desc.append("  ... and ").append(violations.size() - 3).append(" more violations\n");
        }

        return desc.toString().trim();
    }

    /**
     * Generates expected impact description based on priority and scope.
     */
    private String generateExpectedImpact(RecommendationPriority priority, int affectedTypeCount) {
        return switch (priority) {
            case IMMEDIATE ->
                "Resolving this critical issue will restore architectural integrity and prevent system degradation.";
            case SHORT_TERM ->
                String.format(
                        "Addressing these violations will improve maintainability across %d types and reduce future refactoring costs.",
                        affectedTypeCount);
            case MEDIUM_TERM ->
                "Fixing this issue will improve code quality and reduce technical debt for the affected components.";
            case LOW -> "This enhancement will marginally improve code quality with minimal impact on system behavior.";
        };
    }

    /**
     * Formats a constraint name for display (converts kebab-case to title case).
     */
    private String formatConstraintName(ConstraintId constraintId) {
        String name = constraintId.name();
        return capitalizeWords(name.replace("-", " "));
    }

    /**
     * Capitalizes the first letter of each word.
     */
    private String capitalizeWords(String input) {
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
